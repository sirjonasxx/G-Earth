package gearth.protocol.memory;

import gearth.Main;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.crypto.RC4;
import gearth.protocol.memory.habboclient.HabboClient;
import gearth.protocol.memory.habboclient.HabboClientFactory;
import gearth.protocol.packethandler.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Rc4Obtainer {

    public static final boolean DEBUG = false;

    private HabboClient client;
    private List<PacketHandler> packetHandlers;

    public Rc4Obtainer(HConnection hConnection) {
        client = HabboClientFactory.get(hConnection);
    }


    public void setPacketHandlers(PacketHandler... packetHandlers) {
        this.packetHandlers = Arrays.asList(packetHandlers);

        for (PacketHandler handler : packetHandlers) {
            BufferChangeListener bufferChangeListener = new BufferChangeListener() {
                @Override
                public void act() {
                    if (handler.isEncryptedStream()) {
                        onSendFirstEncryptedMessage(handler);
                        handler.removeBufferChangeListener(this);
                    }
                }
            };
            handler.onBufferChanged(bufferChangeListener);
        }


    }



    private void onSendFirstEncryptedMessage(PacketHandler packetHandler) {
        if (!HConnection.DECRYPTPACKETS) return;

        packetHandlers.forEach(PacketHandler::block);

        new Thread(() -> {

            if (DEBUG) System.out.println("[+] send encrypted");

            boolean worked = false;
            int i = 0;
            while (!worked && i < 4) {
                worked = (i % 2 == 0) ?
                        onSendFirstEncryptedMessage(packetHandler, client.getRC4cached()) :
                        onSendFirstEncryptedMessage(packetHandler, client.getRC4possibilities());
                i++;
            }

            if (!worked) {
                System.err.println("COULD NOT FIND RC4 TABLE");


                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Something went wrong!", ButtonType.OK);

                    FlowPane fp = new FlowPane();
                    Label lbl = new Label("G-Earth has experienced an issue" + System.lineSeparator()+ System.lineSeparator() + "Head over to our Troubleshooting page to solve the problem:");
                    Hyperlink link = new Hyperlink("https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting");
                    fp.getChildren().addAll( lbl, link);
                    link.setOnAction(event -> {
                        Main.main.getHostServices().showDocument(link.getText());
                        event.consume();
                    });

                    WebView webView = new WebView();
                    webView.getEngine().loadContent("<html>G-Earth has experienced an issue<br><br>Head over to our Troubleshooting page to solve the problem:<br><a href=\"https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting\">https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting</a></html>");
                    webView.setPrefSize(500, 200);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.getDialogPane().setContent(fp);
                    alert.setOnCloseRequest(event -> {
                        Main.main.getHostServices().showDocument(link.getText());
                    });
                    alert.show();

                });

            }

            packetHandlers.forEach(PacketHandler::unblock);
        }).start();
    }

    private boolean onSendFirstEncryptedMessage(PacketHandler packetHandler, List<byte[]> potentialRC4tables) {
        for (byte[] possible : potentialRC4tables) {

            byte[] encBuffer = new byte[packetHandler.getEncryptedBuffer().size()];
            for (int i = 0; i < encBuffer.length; i++) {
                encBuffer[i] = packetHandler.getEncryptedBuffer().get(i);
            }

            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j++) {
                    byte[] keycpy = Arrays.copyOf(possible, possible.length);
                    RC4 rc4Tryout = new RC4(keycpy, i, j);

                    if (packetHandler.getMessageSide() == HMessage.Side.TOSERVER) rc4Tryout.undoRc4(encBuffer);
                    if (rc4Tryout.couldBeFresh()) {
                        byte[] encDataCopy = Arrays.copyOf(encBuffer, encBuffer.length);
                        RC4 rc4TryCopy = rc4Tryout.deepCopy();

                        try {
                            PayloadBuffer payloadBuffer = new PayloadBuffer();
                            byte[] decoded = rc4TryCopy.rc4(encDataCopy);
                            HPacket[] checker = payloadBuffer.pushAndReceive(decoded);

                            if (payloadBuffer.peak().length == 0) {
                                packetHandler.setRc4(rc4Tryout);
                                return true;
                            }

                        } catch (Exception e) {
//                                e.printStackTrace();
                        }

                    }

                }
            }
        }
        return false;
    }
}
