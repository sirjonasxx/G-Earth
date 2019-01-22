package gearth.extensions;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.ui.extensions.Extensions;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonas on 23/06/18.
 */
public abstract class Extension implements IExtension{

    public interface MessageListener {
        void act(HMessage message);
    }
    public interface FlagsCheckListener {
        void act(String[] args);
    }

    protected boolean canLeave;     // can you disconnect the ext
    protected boolean canDelete;    // can you delete the ext (will be false for some built-in extensions)

    private String[] args;
    private boolean isCorrupted = false;
    private static final String[] PORT_FLAG = {"--port", "-p"};
    private static final String[] FILE_FLAG = {"--filename", "-f"};
    private static final String[] COOKIE_FLAG = {"--auth-token", "-c"}; // don't add a cookie or filename when debugging

    private OutputStream out = null;
    private final Map<Integer, List<MessageListener>> incomingMessageListeners = new HashMap<>();
    private final Map<Integer, List<MessageListener>> outgoingMessageListeners = new HashMap<>();
    private FlagsCheckListener flagRequestCallback = null;

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
        canLeave = canLeave();
        canDelete = canDelete();

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


                if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.INFOREQUEST) {
                    ExtensionInfo info = getInfoAnnotations();

                    HPacket response = new HPacket(Extensions.INCOMING_MESSAGES_IDS.EXTENSIONINFO);
                    response.appendString(info.Title())
                            .appendString(info.Author())
                            .appendString(info.Version())
                            .appendString(info.Description())
                            .appendBoolean(isOnClickMethodUsed())
                            .appendBoolean(file != null)
                            .appendString(file == null ? "": file)
                            .appendString(cookie == null ? "" : cookie)
                            .appendBoolean(canLeave)
                            .appendBoolean(canDelete);
                    writeToStream(response.toBytes());
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.CONNECTIONSTART) {
                    String host = packet.readString();
                    int connectionPort = packet.readInteger();
                    String hotelVersion = packet.readString();
                    String harbleMessagesPath = packet.readString();
                    notifyConnectionListeners(host, connectionPort, hotelVersion, harbleMessagesPath);
                    onStartConnection();
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.CONNECTIONEND) {
                    onEndConnection();
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.FLAGSCHECK) {
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
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.INIT) {
                    initExtension();
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.FREEFLOW) {
                    // nothing to be done yet
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.ONDOUBLECLICK) {
                    onClick();
                }
                else if (packet.headerId() == Extensions.OUTGOING_MESSAGES_IDS.PACKETINTERCEPT) {
                    String stringifiedMessage = packet.readLongString();
                    HMessage habboMessage = new HMessage(stringifiedMessage);
                    HPacket habboPacket = habboMessage.getPacket();

                    Map<Integer, List<MessageListener>> listeners =
                            habboMessage.getDestination() == HMessage.Side.TOCLIENT ?
                                    incomingMessageListeners :
                                    outgoingMessageListeners;

                    List<MessageListener> correctListeners = new ArrayList<>();

                    synchronized (incomingMessageListeners) {
                        synchronized (outgoingMessageListeners) {
                            if (listeners.containsKey(-1)) { // registered on all packets
                                for (int i = listeners.get(-1).size() - 1; i >= 0; i--) {
                                    correctListeners.add(listeners.get(-1).get(i));
                                }
                            }

                            if (listeners.containsKey(habboPacket.headerId())) {
                                for (int i = listeners.get(habboPacket.headerId()).size() - 1; i >= 0; i--) {
                                    correctListeners.add(listeners.get(habboPacket.headerId()).get(i));
                                }
                            }
                        }
                    }

                    for(MessageListener listener : correctListeners) {
                        habboMessage.getPacket().resetReadIndex();
                        listener.act(habboMessage);
                    }
                    habboMessage.getPacket().resetReadIndex();

                    HPacket response = new HPacket(Extensions.INCOMING_MESSAGES_IDS.MANIPULATEDPACKET);
                    response.appendLongString(habboMessage.stringify());

                    writeToStream(response.toBytes());

                }
            }

        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Connection failed; is G-Earth active?");
//            e.printStackTrace();
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
        return send(packet, HMessage.Side.TOCLIENT);
    }

    /**
     * Send a message to the server
     * @param packet packet to be sent
     * @return success or failure
     */
    public boolean sendToServer(HPacket packet) {
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

    /**
     * Register a listener on a specific packet Type
     * @param side ToClient or ToServer
     * @param headerId the packet header ID
     * @param messageListener the callback
     */
    public void intercept(HMessage.Side side, int headerId, MessageListener messageListener) {
        Map<Integer, List<MessageListener>> listeners =
                side == HMessage.Side.TOCLIENT ?
                        incomingMessageListeners :
                        outgoingMessageListeners;

        synchronized (listeners) {
            if (!listeners.containsKey(headerId)) {
                listeners.put(headerId, new ArrayList<>());
            }
        }


        listeners.get(headerId).add(messageListener);
    }

    /**
     * Register a listener on all packets
     * @param side ToClient or ToServer
     * @param messageListener the callback
     */
    public void intercept(HMessage.Side side, MessageListener messageListener) {
        intercept(side, -1, messageListener);
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
        return true;
    }

    /**
     * Write to the console in G-Earth
     * @param s the text to be written
     */
    public void writeToConsole(String s) {
        HPacket packet = new HPacket(Extensions.INCOMING_MESSAGES_IDS.EXTENSIONCONSOLELOG);
        packet.appendString(s);
        try {
            writeToStream(packet.toBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean isOnClickMethodUsed() {

        Class<? extends Extension> c = getClass();

        while (c != Extension.class) {
            try {
                c.getDeclaredMethod("onClick");
                // if it didnt error, onClick exists
                return true;
            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
            }

            c = (Class<? extends Extension>) c.getSuperclass();
        }

        return false;
    }

    /**
     * Gets called when a connection has been established with G-Earth.
     * This does not imply a connection with Habbo is setup.
     */
    protected void initExtension(){}

    /**
     * The application got doubleclicked from the G-Earth interface. Doing something here is optional
     */
    protected void onClick(){}

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

    ExtensionInfo getInfoAnnotations() {
        return getClass().getAnnotation(ExtensionInfo.class);
    }


    public interface OnConnectionListener {
        void act(String host, int port, String hotelversion, String harbleMessagesPath);
    }
    private List<OnConnectionListener> onConnectionListeners = new ArrayList<>();
    public void onConnect(OnConnectionListener listener){
        onConnectionListeners.add(listener);
    }
    private void notifyConnectionListeners(String host, int port, String hotelversion, String harbleMessagesPath) {
        for (OnConnectionListener listener : onConnectionListeners) {
            listener.act(host, port, hotelversion, harbleMessagesPath);
        }
    }

}
