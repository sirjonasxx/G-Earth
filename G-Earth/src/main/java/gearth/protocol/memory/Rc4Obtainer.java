package gearth.protocol.memory;

import gearth.Main;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.crypto.RC4;
import gearth.protocol.memory.habboclient.HabboClient;
import gearth.protocol.memory.habboclient.HabboClientFactory;
import gearth.protocol.packethandler.flash.BufferChangeListener;
import gearth.protocol.packethandler.flash.FlashPacketHandler;
import gearth.protocol.packethandler.PayloadBuffer;
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

public class Rc4Obtainer {

    public static final boolean DEBUG = false;

    private HabboClient client;
    private List<FlashPacketHandler> flashPacketHandlers;

    public Rc4Obtainer(HConnection hConnection) {
        client = HabboClientFactory.get(hConnection);
    }


    public void setFlashPacketHandlers(FlashPacketHandler... flashPacketHandlers) {
        this.flashPacketHandlers = Arrays.asList(flashPacketHandlers);

        for (FlashPacketHandler handler : flashPacketHandlers) {
            BufferChangeListener bufferChangeListener = new BufferChangeListener() {
                @Override
                public void act() {
                    if (handler.isEncryptedStream()) {
                        onSendFirstEncryptedMessage(handler);
                        handler.getBufferChangeObservable().removeListener(this);
                    }
                }
            };
            handler.getBufferChangeObservable().addListener(bufferChangeListener);
        }


    }



    private void onSendFirstEncryptedMessage(FlashPacketHandler flashPacketHandler) {
        if (!HConnection.DECRYPTPACKETS) return;

        flashPacketHandlers.forEach(FlashPacketHandler::block);

        new Thread(() -> {

            long startTime = System.currentTimeMillis();
            if (DEBUG) System.out.println("[+] send encrypted");

            boolean worked = false;
            int i = 0;
            while (!worked && i < 4) {
                worked = (i % 2 == 0) ?
                        onSendFirstEncryptedMessage(flashPacketHandler, client.getRC4cached()) :
                        onSendFirstEncryptedMessage(flashPacketHandler, client.getRC4possibilities());
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

            long endTime = System.currentTimeMillis();
            if (DEBUG) {
                System.out.println("Cracked RC4 in " + (endTime - startTime) + "ms");
            }

            flashPacketHandlers.forEach(FlashPacketHandler::unblock);
        }).start();
    }

    private boolean onSendFirstEncryptedMessage(FlashPacketHandler flashPacketHandler, List<byte[]> potentialRC4tables) {
        for (byte[] possible : potentialRC4tables) {

            byte[] encBuffer = new byte[flashPacketHandler.getEncryptedBuffer().size()];
            for (int i = 0; i < encBuffer.length; i++) {
                encBuffer[i] = flashPacketHandler.getEncryptedBuffer().get(i);
            }

            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j++) {
                    byte[] keycpy = Arrays.copyOf(possible, possible.length);
                    RC4 rc4Tryout = new RC4(keycpy, i, j);

                    if (flashPacketHandler.getMessageSide() == HMessage.Direction.TOSERVER) rc4Tryout.undoRc4(encBuffer);
                    if (rc4Tryout.couldBeFresh()) {
                        byte[] encDataCopy = Arrays.copyOf(encBuffer, encBuffer.length);
                        RC4 rc4TryCopy = rc4Tryout.deepCopy();

                        try {
                            PayloadBuffer payloadBuffer = new PayloadBuffer();
                            byte[] decoded = rc4TryCopy.rc4(encDataCopy);
                            HPacket[] checker = payloadBuffer.pushAndReceive(decoded);

                            if (payloadBuffer.peak().length == 0) {
                                flashPacketHandler.setRc4(rc4Tryout);
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
