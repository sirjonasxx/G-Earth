package gearth.protocol.memory;

import gearth.GEarth;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.crypto.RC4;
import gearth.protocol.crypto.RC4Base64;
import gearth.protocol.memory.habboclient.HabboClient;
import gearth.protocol.memory.habboclient.HabboClientFactory;
import gearth.protocol.packethandler.EncryptedPacketHandler;
import gearth.protocol.packethandler.flash.FlashBuffer;
import gearth.protocol.packethandler.flash.BufferChangeListener;
import gearth.protocol.packethandler.flash.FlashPacketHandler;
import gearth.protocol.packethandler.shockwave.ShockwavePacketOutgoingHandler;
import gearth.protocol.packethandler.PayloadBuffer;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveOutBuffer;
import gearth.ui.titlebar.TitleBarController;
import gearth.ui.translations.LanguageBundle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Rc4Obtainer {

    private static final Logger logger = LoggerFactory.getLogger(Rc4Obtainer.class);

    private final HConnection hConnection;
    private final List<EncryptedPacketHandler> flashPacketHandlers;

    public Rc4Obtainer(HConnection hConnection) {
        this.hConnection = hConnection;
        this.flashPacketHandlers = new ArrayList<>();
    }

    private static void showErrorDialog() {
        Alert alert = new Alert(Alert.AlertType.WARNING, LanguageBundle.get("alert.somethingwentwrong.title"), ButtonType.OK);

        FlowPane fp = new FlowPane();
        Label lbl = new Label(LanguageBundle.get("alert.somethingwentwrong.content").replaceAll("\\\\n", System.lineSeparator()));
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
    }

    public void setFlashPacketHandlers(EncryptedPacketHandler... flashPacketHandlers) {
        this.flashPacketHandlers.addAll(Arrays.asList(flashPacketHandlers));

        for (EncryptedPacketHandler handler : flashPacketHandlers) {
            BufferChangeListener bufferChangeListener = new BufferChangeListener() {
                @Override
                public void onPacket() {
                    if (handler.isEncryptedStream()) {
                        onSendFirstEncryptedMessage(handler);
                        handler.getPacketReceivedObservable().removeListener(this);
                    }
                }
            };
            handler.getPacketReceivedObservable().addListener(bufferChangeListener);
        }
    }

    private void onSendFirstEncryptedMessage(EncryptedPacketHandler flashPacketHandler) {
        if (!HConnection.DECRYPTPACKETS) return;

        flashPacketHandlers.forEach(EncryptedPacketHandler::block);

        logger.info("Caught encrypted packet, attempting to find decryption keys");

        final HabboClient client = HabboClientFactory.get(hConnection);
        if (client == null) {
            logger.info("Unsupported platform / client combination, aborting connection");
            hConnection.abort();
            return;
        }

        new Thread(() -> {
            final long startTime = System.currentTimeMillis();


            boolean worked = false;
            int i = 0;
            while (!worked && i < 4) {
                worked = (i % 2 == 0) ?
                        onSendFirstEncryptedMessage(flashPacketHandler, client.getRC4cached()) :
                        onSendFirstEncryptedMessage(flashPacketHandler, client.getRC4possibilities());
                i++;
            }

            if (!worked) {
                try {
                    Platform.runLater(Rc4Obtainer::showErrorDialog);
                } catch (IllegalStateException e) {
                    // ignore, thrown in tests.
                }

                logger.error("Failed to find RC4 table, aborting connection");
                hConnection.abort();
                return;
            }

            final long endTime = System.currentTimeMillis();
            logger.info("Cracked decryption keys in {}ms", endTime - startTime);

            flashPacketHandlers.forEach(EncryptedPacketHandler::unblock);
        }).start();
    }

    private boolean onSendFirstEncryptedMessage(EncryptedPacketHandler flashPacketHandler, List<byte[]> potentialRC4tables) {
        if (potentialRC4tables == null || potentialRC4tables.isEmpty()) {
            return false;
        }

        for (byte[] possible : potentialRC4tables) {
            if (flashPacketHandler instanceof FlashPacketHandler && bruteFlash(flashPacketHandler, possible))
                return true;

            if (flashPacketHandler instanceof ShockwavePacketOutgoingHandler && bruteShockwaveHeader(flashPacketHandler, possible)) {
                return true;
            }
        }

        return false;
    }

    private boolean bruteShockwaveHeader(EncryptedPacketHandler packetHandler, byte[] tableState) {
        final int encBufferSize = packetHandler.getEncryptedBuffer().size();

        if (encBufferSize < ShockwaveOutBuffer.PACKET_SIZE_MIN_ENCRYPTED) {
            return false;
        }

        // Copy buffer.
        final byte[] encBuffer = new byte[encBufferSize];
        for (int i = 0; i < encBufferSize; i++) {
            encBuffer[i] = packetHandler.getEncryptedBuffer().get(i);
        }

        // Brute force q and j.
        for (int q = 0; q < 256; q++) {
            for (int j = 0; j < 256; j++) {
                final byte[] tableStateCopy = Arrays.copyOf(tableState, tableState.length);
                final RC4Base64 rc4 = new RC4Base64(tableStateCopy, q, j);

                if (packetHandler.getDirection() == HMessage.Direction.TOSERVER) {
                    // Encoded 3 headers, 4 * 3 = 12
                    if (!rc4.undoRc4(12)) {
                        continue;
                    }
                }

                final byte[] encDataCopy = Arrays.copyOf(encBuffer, encBuffer.length);
                final RC4Base64 rc4Test = rc4.deepCopy();

                // Attempt to exhaust buffer.
                final ShockwaveOutBuffer buffer = new ShockwaveOutBuffer();

                buffer.setCipher(rc4Test);
                buffer.push(encDataCopy);

                try {
                    final byte[][] packets = buffer.receive();

                    if (packets.length == 3 && buffer.isEmpty()) {
                        packetHandler.setRc4(rc4);
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    // ignore
                }
            }
        }

        return false;
    }

    private boolean bruteFlash(EncryptedPacketHandler flashPacketHandler, byte[] possible) {

        try {

            final byte[] encBuffer = new byte[flashPacketHandler.getEncryptedBuffer().size()];

            for (int i = 0; i < encBuffer.length; i++)
                encBuffer[i] = flashPacketHandler.getEncryptedBuffer().get(i);

            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j++) {

                    final byte[] keycpy = Arrays.copyOf(possible, possible.length);
                    final RC4 rc4Tryout = new RC4(keycpy, i, j);

                    if (flashPacketHandler.getDirection() == HMessage.Direction.TOSERVER)
                        rc4Tryout.undoRc4(encBuffer);

                    if (rc4Tryout.couldBeFresh()) {

                        final byte[] encDataCopy = Arrays.copyOf(encBuffer, encBuffer.length);
                        final RC4 rc4TryCopy = rc4Tryout.deepCopy();

                        try {
                            final PayloadBuffer payloadBuffer = new FlashBuffer();
                            final byte[] decoded = rc4TryCopy.cipher(encDataCopy);

                            payloadBuffer.push(decoded);
                            payloadBuffer.receive();

                            if (payloadBuffer.isEmpty()) {
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
