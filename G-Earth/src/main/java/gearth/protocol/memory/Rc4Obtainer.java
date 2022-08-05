package gearth.protocol.memory;

import gearth.GEarth;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.crypto.RC4;
import gearth.protocol.memory.habboclient.HabboClient;
import gearth.protocol.memory.habboclient.HabboClientFactory;
import gearth.protocol.packethandler.PayloadBuffer;
import gearth.protocol.packethandler.flash.BufferChangeListener;
import gearth.protocol.packethandler.flash.FlashPacketHandler;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Rc4Obtainer {

    public static final boolean DEBUG = false;

    private final HabboClient client;
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
                    Alert alert = new Alert(Alert.AlertType.WARNING, GEarth.translation.getString("alert.somethingwentwrong.title"), ButtonType.OK);

                    FlowPane fp = new FlowPane();
                    Label lbl = new Label(GEarth.translation.getString("alert.somethingwentwrong.content").replaceAll("\\\\n", System.lineSeparator()));
                    Hyperlink link = new Hyperlink("https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting");
                    fp.getChildren().addAll(lbl, link);
                    link.setOnAction(event -> {
                        GEarth.main.getHostServices().showDocument(link.getText());
                        event.consume();
                    });

                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.getDialogPane().setContent(fp);
                    alert.setOnCloseRequest(event -> GEarth.main.getHostServices().showDocument(link.getText()));
                    try {
                        TitleBarController.create(alert).showAlert();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

            final long endTime = System.currentTimeMillis();
            if (DEBUG)
                System.out.println("Cracked RC4 in " + (endTime - startTime) + "ms");

            flashPacketHandlers.forEach(FlashPacketHandler::unblock);
        }).start();
    }

    private boolean onSendFirstEncryptedMessage(FlashPacketHandler flashPacketHandler, List<byte[]> potentialRC4tables) {

        for (byte[] possible : potentialRC4tables)
            if (isCorrectRC4Table(flashPacketHandler, possible))
                return true;

        return false;
    }

    private boolean isCorrectRC4Table(FlashPacketHandler flashPacketHandler, byte[] possible) {

        try {

            final byte[] encBuffer = new byte[flashPacketHandler.getEncryptedBuffer().size()];

            for (int i = 0; i < encBuffer.length; i++)
                encBuffer[i] = flashPacketHandler.getEncryptedBuffer().get(i);

            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j++) {

                    final byte[] keycpy = Arrays.copyOf(possible, possible.length);
                    final RC4 rc4Tryout = new RC4(keycpy, i, j);

                    if (flashPacketHandler.getMessageSide() == HMessage.Direction.TOSERVER)
                        rc4Tryout.undoRc4(encBuffer);

                    if (rc4Tryout.couldBeFresh()) {

                        final byte[] encDataCopy = Arrays.copyOf(encBuffer, encBuffer.length);
                        final RC4 rc4TryCopy = rc4Tryout.deepCopy();

                        try {
                            final PayloadBuffer payloadBuffer = new PayloadBuffer();
                            final byte[] decoded = rc4TryCopy.rc4(encDataCopy);

                            payloadBuffer.pushAndReceive(decoded);

                            if (payloadBuffer.peak().length == 0) {
                                flashPacketHandler.setRc4(rc4Tryout);
                                return true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
