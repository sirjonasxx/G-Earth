package main.extensions;

import main.protocol.HMessage;
import main.protocol.HPacket;
import main.protocol.packethandler.PayloadBuffer;
import main.ui.extensions.Extensions;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonas on 23/06/18.
 */
public abstract class Extension {

    public interface MessageListener {
        void act(HMessage message);
    }
    public interface FlagsCheckListener {
        void act(String[] args);
    }


    private static final String[] PORT_FLAG = {"--port", "-p"};

    private OutputStream out = null;
    private Map<Integer, List<MessageListener>> incomingMessageListeners = new HashMap<>();
    private Map<Integer, List<MessageListener>> outgoingMessageListeners = new HashMap<>();
    private FlagsCheckListener flagRequestCallback = null;



    public Extension(String[] args) {
        //obtain port
        int port = 0;

        outerloop:
        for (int i = 0; i < args.length - 1; i++) {
            for (String str : PORT_FLAG) {
                if (args[i].equals(str)) {
                    port = Integer.parseInt(args[i+1]);
                    break outerloop;
                }
            }
        }

        HPacket lastwrapper = null;
        HMessage lastM = null;
        HPacket last = null;


        Socket gEarthExtensionServer = null;
        try {
            gEarthExtensionServer = new Socket("localhost", port);

            InputStream in = gEarthExtensionServer.getInputStream();
            DataInputStream dIn = new DataInputStream(in);
            out = gEarthExtensionServer.getOutputStream();

            while (!gEarthExtensionServer.isClosed()) {

                int length = dIn.readInt();
                byte[] headerandbody = new byte[length + 4];

                int amountRead = 0;
                while (amountRead < length) {
                    amountRead += dIn.read(headerandbody, 4 + amountRead, Math.min(dIn.available(), length - amountRead));
                }

                HPacket packet = new HPacket(headerandbody);
                packet.fixLength();


                if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.INFOREQUEST) {
                    HPacket response = new HPacket(Extensions.INCOMING_MESSAGES_IDS.EXTENSIONINFO);
                    response.appendString(getTitle())
                            .appendString(getAuthor())
                            .appendString(getVersion())
                            .appendString(getDescription());
                    writeToStream(response.toBytes());
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.CONNECTIONSTART) {
                    onStartConnection();
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.CONNECTIONEND) {
                    onEndConnection();
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.FLAGSCHECK) {
                    // body = an array of G-Earths main flags
                    if (flagRequestCallback != null) {
                        int arraysize = packet.readInteger();
                        String[] gEarthArgs = new String[arraysize];
                        for (int i = 0; i < gEarthArgs.length; i++) {
                            gEarthArgs[i] = packet.readString();
                        }
                        flagRequestCallback.act(gEarthArgs);
                    }
                    flagRequestCallback = null;
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.INIT) {
                    init();
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.FREEFLOW) {
                    // nothing to be done yet
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.ONDOUBLECLICK) {
                    onDoubleClick();
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.PACKETINTERCEPT) {
                    String stringifiedMessage = packet.readLongString();
                    HMessage habboMessage = new HMessage(stringifiedMessage);
                    HPacket habboPacket = habboMessage.getPacket();

                    lastwrapper = packet;
                    lastM = habboMessage;
                    last = habboPacket;

                    Map<Integer, List<MessageListener>> listeners =
                            habboMessage.getDestination() == HMessage.Side.TOCLIENT ?
                                    incomingMessageListeners :
                                    outgoingMessageListeners;

                    if (listeners.containsKey(-1)) { // registered on all packets
                        for (int i = listeners.get(-1).size() - 1; i >= 0; i--) {
                            listeners.get(-1).get(i).act(habboMessage);
                        }
                    }

                    if (listeners.containsKey(habboPacket.headerId())) {
                        for (int i = listeners.get(habboPacket.headerId()).size() - 1; i >= 0; i--) {
                            listeners.get(habboPacket.headerId()).get(i).act(habboMessage);
                        }
                    }

                    HPacket response = new HPacket(Extensions.INCOMING_MESSAGES_IDS.MANIPULATEDPACKET);
                    response.appendLongString(habboMessage.stringify());

                    writeToStream(response.toBytes());
                }
            }


        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        finally {
            if (gEarthExtensionServer != null && !gEarthExtensionServer.isClosed()) {
                try {
                    gEarthExtensionServer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void writeToStream(byte[] bytes) throws IOException {
        synchronized (out) {
            out.write(bytes);
        }
    }

    //methods returns if succeed
    protected boolean sendToClient(HPacket packet) {
        return send(packet, HMessage.Side.TOCLIENT);
    }
    protected boolean sendToServer(HPacket packet) {
        return send(packet, HMessage.Side.TOSERVER);
    }
    private boolean send(HPacket packet, HMessage.Side side) {
        HPacket packet1 = new HPacket(Extensions.INCOMING_MESSAGES_IDS.SENDMESSAGE);
        packet1.appendByte(side == HMessage.Side.TOCLIENT ? (byte)0 : (byte)1);
        packet1.appendInt(packet.getBytesLength());
        packet1.appendBytes(packet.toBytes());
        try {
            writeToStream(packet1.toBytes());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    protected void intercept(HMessage.Side side, int headerId, MessageListener messageListener) {
        Map<Integer, List<MessageListener>> listeners =
                side == HMessage.Side.TOCLIENT ?
                        incomingMessageListeners :
                        outgoingMessageListeners;

        if (!listeners.containsKey(headerId)) {
            listeners.put(headerId, new ArrayList<>());
        }

        listeners.get(headerId).add(messageListener);
    }
    protected void intercept(HMessage.Side side, MessageListener messageListener) {
        intercept(side, -1, messageListener);
    }

    //returns "false" if another flag requester was busy
    protected boolean requestFlags(FlagsCheckListener flagRequestCallback) {
        if (this.flagRequestCallback != null) return false;
        this.flagRequestCallback = flagRequestCallback;
        return true;
    }

    protected void writeToConsole(String s) {
        HPacket packet = new HPacket(Extensions.INCOMING_MESSAGES_IDS.EXTENSIONCONSOLELOG);
        packet.appendString(s);
        try {
            writeToStream(packet.toBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // All methods under here block the stream, use threads if needed.
    protected abstract void init();
    protected abstract void onDoubleClick();
    protected abstract void onStartConnection();
    protected abstract void onEndConnection();

    protected abstract String getTitle();
    protected abstract String getDescription();
    protected abstract String getVersion();
    protected abstract String getAuthor();
}
