package gearth.ui.extensions;

import gearth.misc.harble_api.HarbleAPIFetcher;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import gearth.Main;
import gearth.protocol.*;
import gearth.ui.SubForm;
import gearth.ui.extensions.executer.ExecutionInfo;
import gearth.ui.extensions.executer.ExtensionRunner;
import gearth.ui.extensions.executer.ExtensionRunnerFactory;
import gearth.ui.scheduler.ScheduleItem;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jonas on 06/04/18.
 */


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


public class Extensions extends SubForm {


    public Button btn_install;
    public Button btn_remove;
    public TextField ext_port;
    public VBox extensioncontainer;
    public GridPane header_ext;
    public ScrollPane scroller;

    private ExtensionRunner extensionRunner = null;
    private GEarthExtensionsRegistrer extensionsRegistrer = null;

    public static class OUTGOING_MESSAGES_IDS {
        public static final int ONDOUBLECLICK = 1;
        public static final int INFOREQUEST = 2;        // backend: implemented
        public static final int PACKETINTERCEPT = 3;    // backend: implemented
        public static final int FLAGSCHECK = 4;         // backend: implemented
        public static final int CONNECTIONSTART = 5;    // backend: implemented
        public static final int CONNECTIONEND = 6;      // backend: implemented
        public static final int INIT = 7;               // backend: implemented
        public static final int FREEFLOW = 99;          // no implementation needed yet
    }


    public static class INCOMING_MESSAGES_IDS {
        public static final int EXTENSIONINFO = 1;      // backend: implemented
        public static final int MANIPULATEDPACKET = 2;  // backend: implemented
        public static final int REQUESTFLAGS = 3;       // backend: implemented
        public static final int SENDMESSAGE = 4;        // backend: implemented
        public static final int EXTENSIONCONSOLELOG = 98;
        public static final int FREEFLOW = 99;          // no implementation needed yet
    }



    private final List<GEarthExtension> gEarthExtensions = new ArrayList<>();

    public void initialize() {
        scroller.widthProperty().addListener(observable -> header_ext.setPrefWidth(scroller.getWidth()));
    }

    protected void onParentSet() {
        ExtensionItemContainerProducer producer = new ExtensionItemContainerProducer(extensioncontainer, scroller);

        getHConnection().addStateChangeListener((oldState, newState) -> {
            if (newState == HConnection.State.CONNECTED) {
                HarbleAPIFetcher.fetch(getHConnection().getHotelVersion());
                synchronized (gEarthExtensions) {
                    for (GEarthExtension extension : gEarthExtensions) {
                        extension.sendMessage(
                                new HPacket(OUTGOING_MESSAGES_IDS.CONNECTIONSTART)
                                        .appendString(getHConnection().getDomain())
                                        .appendInt(getHConnection().getPort())
                                        .appendString(getHConnection().getHotelVersion())
                                        .appendString(HarbleAPIFetcher.HARBLEAPI + "") // automatic .toString() and no additional nullpointerexception
                        );
                    }
                }
            }
            if (oldState == HConnection.State.CONNECTED) {
                synchronized (getHConnection()) {
                    for (GEarthExtension extension : gEarthExtensions) {
                        extension.sendMessage(new HPacket(OUTGOING_MESSAGES_IDS.CONNECTIONEND));
                    }
                }
            }
        });

        getHConnection().addTrafficListener(1, message -> {
            Set<GEarthExtension> collection;
            synchronized (gEarthExtensions) {
                collection = new HashSet<>(gEarthExtensions);
            }

            String stringified = message.stringify();
            HPacket manipulatePacketRequest = new HPacket(OUTGOING_MESSAGES_IDS.PACKETINTERCEPT);
            manipulatePacketRequest.appendLongString(stringified);

            HMessage result = new HMessage(message);

            boolean[] isblock = new boolean[1];
            synchronized (collection) {
                for (GEarthExtension extension : collection) {
                    GEarthExtension.ReceiveMessageListener respondCallback = new GEarthExtension.ReceiveMessageListener() {
                        @Override
                        public void act(HPacket packet) {
                            if (packet.headerId() == INCOMING_MESSAGES_IDS.MANIPULATEDPACKET) {
                                String stringifiedresponse = packet.readLongString(6);
                                HMessage responseMessage = new HMessage(stringifiedresponse);
                                if (responseMessage.getDestination() == message.getDestination() && responseMessage.getIndex() == message.getIndex()) {
                                    synchronized (result) {
                                        if (!message.equals(responseMessage)) {
                                            result.constructFromString(stringifiedresponse);
                                        }
                                        if (responseMessage.isBlocked()) {
                                            isblock[0] = true;
                                        }
                                        synchronized (collection) {
                                            collection.remove(extension);
                                        }

                                        extension.removeOnReceiveMessageListener(this);
                                    }
                                }
                            }

                        }
                    };
                    extension.addOnReceiveMessageListener(respondCallback);
                    extension.sendMessage(manipulatePacketRequest);
                }
            }

            //block untill all extensions have responded
            List<GEarthExtension> willdelete = new ArrayList<>();
            while (true) {
                synchronized (collection) {
                    if (collection.isEmpty()) {
                        break;
                    }

                    synchronized (gEarthExtensions) {
                        for (GEarthExtension extension : collection) {
                            if (!gEarthExtensions.contains(extension)) willdelete.add(extension);
                        }
                    }

                    for (int i = willdelete.size() - 1; i >= 0; i--) {
                        collection.remove(willdelete.get(i));
                        willdelete.remove(i);
                    }
                }


                try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
            }

            message.constructFromHMessage(result);

            if (isblock[0]) {
                message.setBlocked(true);
            }
        });


        HashMap<GEarthExtension, GEarthExtension.ReceiveMessageListener> messageListeners = new HashMap<>();
        try {
            extensionsRegistrer = new GEarthExtensionsRegistrer(new GEarthExtensionsRegistrer.ExtensionRegisterObserver() {
                @Override
                public void onConnect(GEarthExtension extension) {
                    synchronized (gEarthExtensions) {
                        gEarthExtensions.add(extension);
                    }

                    GEarthExtension.ReceiveMessageListener receiveMessageListener = message -> {
                        if (message.headerId() == INCOMING_MESSAGES_IDS.REQUESTFLAGS) { // no body
                            HPacket packet = new HPacket(OUTGOING_MESSAGES_IDS.FLAGSCHECK);
                            packet.appendInt(Main.args.length);
                            for (String arg : Main.args) {
                                packet.appendString(arg);
                            }
                            extension.sendMessage(packet);
                        }
                        else if (message.headerId() == INCOMING_MESSAGES_IDS.SENDMESSAGE) {
                            Byte side = message.readByte();
                            int byteLength = message.readInteger();
                            byte[] packetAsByteArray = message.readBytes(byteLength);

                            HPacket packet = new HPacket(packetAsByteArray);
                            if (!packet.isCorrupted()) {
                                if (side == 0) {        // toclient
                                    getHConnection().sendToClientAsync(packet);
                                }
                                else if (side == 1) {   // toserver
                                    getHConnection().sendToServerAsync(packet);
                                }
                            }
                        }
                    };
                    synchronized (messageListeners) {
                        messageListeners.put(extension, receiveMessageListener);
                    }
                    extension.addOnReceiveMessageListener(receiveMessageListener);

                    extension.sendMessage(new HPacket(OUTGOING_MESSAGES_IDS.INIT));
                    if (getHConnection().getState() == HConnection.State.CONNECTED) {
                        extension.sendMessage(
                                new HPacket(OUTGOING_MESSAGES_IDS.CONNECTIONSTART)
                                        .appendString(getHConnection().getDomain())
                                        .appendInt(getHConnection().getPort())
                                        .appendString(getHConnection().getHotelVersion())
                                        .appendString(HarbleAPIFetcher.HARBLEAPI + "")
                        );
                    }

                    extension.onRemoveClick(observable -> {
                        try {
                            extension.getConnection().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    extension.onClick(observable -> extension.sendMessage(new HPacket(OUTGOING_MESSAGES_IDS.ONDOUBLECLICK)));

                    Platform.runLater(() -> producer.extensionConnected(extension));
                }

                @Override
                public void onDisconnect(GEarthExtension extension) {
                    synchronized (gEarthExtensions) {
                        gEarthExtensions.remove(extension);
                    }

                    synchronized (messageListeners) {
                    extension.removeOnReceiveMessageListener(messageListeners.get(extension));
                        messageListeners.remove(extension);
                    }
                    Platform.runLater(extension::delete);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        producer.setPort(extensionsRegistrer.getPort());
        ext_port.setText(extensionsRegistrer.getPort()+"");
//        System.out.println("Extension server registered on port: " + extensionsRegistrer.getPort());

        extensionRunner = ExtensionRunnerFactory.get();
        extensionRunner.runAllExtensions(extensionsRegistrer.getPort());
    }


    public void installBtnClicked(ActionEvent actionEvent) {
        List<ScheduleItem> list = new ArrayList<>();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Install extension");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("G-Earth extensions", ExecutionInfo.ALLOWEDEXTENSIONTYPES));
        File selectedFile = fileChooser.showOpenDialog(parentController.getStage());
        if (selectedFile != null) {
            extensionRunner.installAndRunExtension(selectedFile.getPath(), extensionsRegistrer.getPort());
        }
    }
}
