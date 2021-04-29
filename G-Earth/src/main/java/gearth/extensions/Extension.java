package gearth.extensions;

import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.services.Constants;
import gearth.services.extensionhandler.extensions.implementations.network.NetworkExtensionInfo;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by Jonas on 23/06/18.
 */
public abstract class Extension extends ExtensionBase {

    protected FlagsCheckListener flagRequestCallback = null;

    private String[] args;
    private boolean isCorrupted = false;
    private static final String[] PORT_FLAG = {"--port", "-p"};
    private static final String[] FILE_FLAG = {"--filename", "-f"};
    private static final String[] COOKIE_FLAG = {"--auth-token", "-c"}; // don't add a cookie or filename when debugging

    protected PacketInfoManager packetInfoManager = new PacketInfoManager(new ArrayList<>()); // empty

    private OutputStream out = null;

    private String getArgument(String[] args, String... arg) {
        for (int i = 0; i < args.length - 1; i++) {
            for (String str : arg) {
                if (args[i].toLowerCase().equals(str.toLowerCase())) {
                    return args[i+1];
                }
            }
        }
        return null;
    }

    /**
     * Makes the connection with G-Earth, pass the arguments given in the Main method "super(args)"
     * @param args arguments
     */
    public Extension(String[] args) {
        super();

        //obtain port
        this.args = args;


        if (getInfoAnnotations() == null) {
            System.err.println("Extension info not found\n\n" +
                    "Usage:\n" +
                    "@ExtensionInfo ( \n" +
                    "       Title =  \"...\",\n" +
                    "       Description =  \"...\",\n" +
                    "       Version =  \"...\",\n" +
                    "       Author =  \"...\"" +
                    "\n)");
            isCorrupted = true;
        }

        if (getArgument(args, PORT_FLAG) == null) {
            System.err.println("Don't forget to include G-Earth's port in your program parameters (-p {port})");
            isCorrupted = true;
        }
    }

    public void run() {
        if (isCorrupted) {
            return;
        }

        int port = Integer.parseInt(getArgument(args, PORT_FLAG));
        String file = getArgument(args, FILE_FLAG);
        String cookie = getArgument(args, COOKIE_FLAG);

        Socket gEarthExtensionServer = null;
        try {
            gEarthExtensionServer = new Socket("127.0.0.1", port);
            gEarthExtensionServer.setTcpNoDelay(true);
            InputStream in = gEarthExtensionServer.getInputStream();
            DataInputStream dIn = new DataInputStream(in);
            out = gEarthExtensionServer.getOutputStream();

            while (!gEarthExtensionServer.isClosed()) {

                int length;
                try {
                    length = dIn.readInt();
                }
                catch(EOFException exception) {
                    //g-earth closed the extension
                    break;
                }

                byte[] headerandbody = new byte[length + 4];

                int amountRead = 0;

                while (amountRead < length) {
                    amountRead += dIn.read(headerandbody, 4 + amountRead, Math.min(dIn.available(), length - amountRead));
                }

                HPacket packet = new HPacket(headerandbody);
                packet.fixLength();


                if (packet.headerId() == NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.INFOREQUEST) {
                    ExtensionInfo info = getInfoAnnotations();

                    HPacket response = new HPacket(NetworkExtensionInfo.INCOMING_MESSAGES_IDS.EXTENSIONINFO);
                    response.appendString(info.Title())
                            .appendString(info.Author())
                            .appendString(info.Version())
                            .appendString(info.Description())
                            .appendBoolean(isOnClickMethodUsed())
                            .appendBoolean(file != null)
                            .appendString(file == null ? "": file)
                            .appendString(cookie == null ? "" : cookie)
                            .appendBoolean(canLeave())
                            .appendBoolean(canDelete());
                    writeToStream(response.toBytes());
                }
                else if (packet.headerId() == NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.CONNECTIONSTART) {
                    String host = packet.readString();
                    int connectionPort = packet.readInteger();
                    String hotelVersion = packet.readString();
                    String clientIdentifier = packet.readString();
                    HClient clientType = HClient.valueOf(packet.readString());
                    packetInfoManager = PacketInfoManager.readFromPacket(packet);

                    Constants.UNITY_PACKETS = clientType == HClient.UNITY;
                    getOnConnectionObservable().fireEvent(l -> l.onConnection(
                            host, connectionPort, hotelVersion,
                            clientIdentifier, clientType, packetInfoManager)
                    );
                    onStartConnection();
                }
                else if (packet.headerId() == NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.CONNECTIONEND) {
                    onEndConnection();
                }
                else if (packet.headerId() == NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.FLAGSCHECK) {
                    // body = an array of G-Earths gearth flags
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
                else if (packet.headerId() == NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.INIT) {
//                    boolean isConnected = packet.readBoolean(); - don't read since not relevant here
                    initExtension();
                    writeToConsole("green","Extension \"" + getInfoAnnotations().Title() + "\" successfully initialized", false);
                }
                else if (packet.headerId() == NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.ONDOUBLECLICK) {
                    onClick();
                }
                else if (packet.headerId() == NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.PACKETINTERCEPT) {
                    String stringifiedMessage = packet.readLongString();
                    HMessage habboMessage = new HMessage(stringifiedMessage);

                    modifyMessage(habboMessage);

                    HPacket response = new HPacket(NetworkExtensionInfo.INCOMING_MESSAGES_IDS.MANIPULATEDPACKET);
                    response.appendLongString(habboMessage.stringify());

                    writeToStream(response.toBytes());

                }
            }

        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Connection failed; is G-Earth active?");
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
        synchronized (this) {
            out.write(bytes);
        }
    }

    /**
     * Send a message to the client
     * @param packet packet to be sent
     * @return success or failure
     */
    public boolean sendToClient(HPacket packet) {
        return send(packet, HMessage.Direction.TOCLIENT);
    }

    /**
     * Send a message to the server
     * @param packet packet to be sent
     * @return success or failure
     */
    public boolean sendToServer(HPacket packet) {
        return send(packet, HMessage.Direction.TOSERVER);
    }
    private boolean send(HPacket packet, HMessage.Direction direction) {
        if (packet.isCorrupted()) return false;

        if (!packet.isPacketComplete()) packet.completePacket(packetInfoManager);
        if (!packet.isPacketComplete()) return false;

        HPacket packet1 = new HPacket(NetworkExtensionInfo.INCOMING_MESSAGES_IDS.SENDMESSAGE);
        packet1.appendByte(direction == HMessage.Direction.TOCLIENT ? (byte)0 : (byte)1);
        packet1.appendInt(packet.getBytesLength());
        packet1.appendBytes(packet.toBytes());
        try {
            writeToStream(packet1.toBytes());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Requests the flags which have been given to G-Earth when it got executed
     * For example, you might want this extension to do a specific thing if the flag "-e" was given
     * @param flagRequestCallback callback
     * @return if the request was successful, will return false if another flagrequest is busy
     */
    public boolean requestFlags(FlagsCheckListener flagRequestCallback) {
        if (this.flagRequestCallback != null) return false;
        this.flagRequestCallback = flagRequestCallback;
        try {
            writeToStream(new HPacket(NetworkExtensionInfo.INCOMING_MESSAGES_IDS.REQUESTFLAGS).toBytes());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Write to the console in G-Earth
     * @param s the text to be written
     * @param colorClass color of the text to be written
     */
    public void writeToConsole(String colorClass, String s) {
        writeToConsole(colorClass, s, true);
    }

    /**
     * Write to the console in G-Earth
     * @param s the text to be written
     * @param colorClass color of the text to be written
     * @param mentionTitle log the extension title as well
     */
    private void writeToConsole(String colorClass, String s, boolean mentionTitle) {
        String text = "[" + colorClass + "]" + (mentionTitle ? (getInfoAnnotations().Title() + " --> ") : "") + s;

        HPacket packet = new HPacket(NetworkExtensionInfo.INCOMING_MESSAGES_IDS.EXTENSIONCONSOLELOG);
        packet.appendString(text);
        try {
            writeToStream(packet.toBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets called when a connection has been established with G-Earth.
     * This does not imply a connection with Habbo is setup.
     */
    protected void initExtension(){}

    /**
     * A connection with Habbo has been started
     */
    protected void onStartConnection(){}

    /**
     * A connection with Habbo has ended
     */
    protected void onEndConnection(){}

    protected boolean canLeave() {
        return true;
    }

    protected boolean canDelete() {
        return true;
    }

}
