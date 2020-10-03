package gearth.services.extensionhandler.extensions.implementations.network;

public class NetworkExtensionInfo {

    /**
     * THE EXTENSION COMMUNCATION PRINCIPLES & PROTOCOL:
     *
     * You will be able to write extensions in ANY language you want, but we will only provide an interface
     * for Java so if you write your own in for example Python, make SURE you do it correctly or it could fuck G-Earth.
     *
     * Also, don't let the method where you manipulate the packets block. Similiar as how you must not block things in an UI thread.
     * Why? Because Habbo relies on the TCP protocol, which ENSURES that packets get received in the right order, so we will not be fucking that up.
     * That means that all packets following the packet you're manipulating in your extension will be blocked from being sent untill you're done.
     * TIP: If you're trying to replace a packet in your extension but you know it will take time, just block the packet, end the method, and let something asynchronous send
     * the editted packet when you're done.
     *
     *
     * You may ignore everything beneath this line if you're extending the abstract Extension class we provide in Java.
     * -----------------------------------------------------------------------------------------------------------------
     *
     * (0. We recommend to use a cross-platform language for your extension)
     *
     * 1.   An extension will run as a seperate process on your device and has to be called with the flag "-p <PORT>",
     *      where <PORT> is a random port where the G-Earth local extension server will run on. Your extension has to connect with this server.
     *
     * 2.   G-Earth will open your program only ONCE, that is on the boot of G-Earth or when you install the exension.
     *      Same story goes for closing the connection between the program and G-Earth, only once (on uninstall or close of G-Earth).
     *
     *      You may also run your extension completely seperate from G-Earth for debugging purpose for example, then it won't be installed in G-Earth
     *      (but you have to configure the port yourself, which will be displayed in the extension page)
     *
     * 3.   Once a connection is made, your extension will have to deal with the following incoming & outgoing messages as described (follows the same protocol structure as Habbo communication does):
     *      (if an object is sent; the object will be sent with its String representation from the StringifyAble interface, so the object's class must implement that)
     *
     *      INCOMING MESSAGES: (marked with * if you're required to correctly respond or take action, ** if it's a response on something you requested)
     *      -----------------------------------------------------------------------------------------------------
     *      |  ID  |       TITLE        |                         BODY & DESCRIPTION                            |
     *      -----------------------------------------------------------------------------------------------------
     *      |  1   |   ON-DOUBLECLICK   |  No body, the extension has been double clicked from within G-Earth   |  ( <- typically for tanji-module-like extensions you will open the UI here)
     *      -----------------------------------------------------------------------------------------------------
     *      |  2   |    INFO-REQUEST*   | Needs response with extension info (name, desc, author, version, ..), |
     *      |      |                    |  exact implementation is found in the Java abstract Extension class   |
     *      -----------------------------------------------------------------------------------------------------
     *      |  3   | PACKET-INTERCEPT*  |       Includes the whole HMessage as body, needs response with the    |
     *      |      |                    |                  manipulated HMessage (OUTGOING id: 2)                |
     *      -----------------------------------------------------------------------------------------------------
     *      |  4   |   FLAGS-CHECK**    | Body: String with G-Earth's boot flags (args from static gearth method) |
     *      -----------------------------------------------------------------------------------------------------
     *      |  5   |  CONNECTION START  |             just a note that a new connection has been made,          |
     *      |      |                    |   you could check this yourself as well (listen to out:4000 packet)   |
     *      |      |                    |                      host/port, hotel version                         |
     *      -----------------------------------------------------------------------------------------------------
     *      |  6   |   CONNECTION END   |        Empty body, just a note that a connection has ended            |
     *      -----------------------------------------------------------------------------------------------------
     *      |  7   |        INIT        |        Empty body, a connection with G-Earth has been set up          |
     *      -----------------------------------------------------------------------------------------------------
     *      |  99  |     FREE FLOW      |                      extension-specific body                          |
     *      -----------------------------------------------------------------------------------------------------
     *
     *      OUTGOING MESSAGES: (marked with * if that is a response to one of the msgs above)
     *      -----------------------------------------------------------------------------------------------------
     *      |  ID  |       TITLE        |                         BODY & DESCRIPTION                            |
     *      -----------------------------------------------------------------------------------------------------
     *      |  1   |  EXTENSION-INFO*   |                      Response for INFO-REQUEST                        |
     *      -----------------------------------------------------------------------------------------------------
     *      |  2   | MANIPULATED-PACKET*|                    Response for PACKET-INTERCEPT                      |
     *      -----------------------------------------------------------------------------------------------------
     *      |  3   |   REQUEST-FLAGS    |  Request G-Earth's flags, results in incoming FLAGS-CHECK response    |
     *      -----------------------------------------------------------------------------------------------------
     *      |  4   |    SEND-MESSAGE    |   Body: HMessage object. Sends the HPacket wrapped in the HMessage    |
     *      |      |                    |                        to the client/server                           |
     *      -----------------------------------------------------------------------------------------------------
     *      |  99  |     FREE FLOW      |                      extension-specific body                          |
     *      -----------------------------------------------------------------------------------------------------
     *
     * 4.   Your extension will only appear in the extension list once the EXTENSION-INFO has been received by G-Earth
     *
     *
     */


    public static class OUTGOING_MESSAGES_IDS {
        public static final int ONDOUBLECLICK = 1;
        public static final int INFOREQUEST = 2;
        public static final int PACKETINTERCEPT = 3;
        public static final int FLAGSCHECK = 4;
        public static final int CONNECTIONSTART = 5;
        public static final int CONNECTIONEND = 6;
        public static final int INIT = 7;

        public static final int PACKETTOSTRING_RESPONSE = 20;
        public static final int STRINGTOPACKET_RESPONSE = 21;
    }


    public static class INCOMING_MESSAGES_IDS {
        public static final int EXTENSIONINFO = 1;
        public static final int MANIPULATEDPACKET = 2;
        public static final int REQUESTFLAGS = 3;
        public static final int SENDMESSAGE = 4;

        public static final int PACKETTOSTRING_REQUEST = 20;
        public static final int STRINGTOPACKET_REQUEST = 21;

        public static final int EXTENSIONCONSOLELOG = 98;
    }

}
