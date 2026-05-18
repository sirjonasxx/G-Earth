package gearth.app.protocol.memory;

import gearth.app.GEarth;
import gearth.app.protocol.HConnection;
import gearth.app.protocol.crypto.RC4;
import gearth.app.protocol.memory.habboclient.HabboClient;
import gearth.app.protocol.memory.habboclient.HabboClientFactory;
import gearth.app.protocol.packethandler.EncryptedPacketHandler;
import gearth.app.protocol.packethandler.PayloadBuffer;
import gearth.app.protocol.packethandler.flash.BufferChangeListener;
import gearth.app.protocol.packethandler.flash.FlashBuffer;
import gearth.app.protocol.packethandler.flash.FlashPacketHandler;
import gearth.app.protocol.packethandler.shockwave.buffers.ShockwaveProperBuffer;
import gearth.app.ui.titlebar.TitleBarAlert;
import gearth.app.ui.translations.LanguageBundle;
import gearth.protocol.HMessage;
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
import java.util.concurrent.atomic.AtomicInteger;

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
            TitleBarAlert.create(alert).showAlert();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setFlashPacketHandlers(EncryptedPacketHandler... flashPacketHandlers) {
        this.flashPacketHandlers.addAll(Arrays.asList(flashPacketHandlers));

        for (EncryptedPacketHandler handler : flashPacketHandlers) {
            BufferChangeListener bufferChangeListener = new BufferChangeListener() {
                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public void onPacket() {
                    if (handler.isEncryptedStream()) {
                        if (counter.incrementAndGet() != 3) {
                            return;
                        }

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

        new Thread(() -> {
            final long startTime = System.currentTimeMillis();

            if (!onSendFirstEncryptedMessage(flashPacketHandler, client.getRC4Tables())) {
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

    private boolean onSendFirstEncryptedMessage(EncryptedPacketHandler packetHandler, List<byte[]> potentialRC4tables) {
        logger.info("Attempting to brute force RC4 table");

        if (potentialRC4tables == null || potentialRC4tables.isEmpty()) {
            logger.debug("No RC4 tables available");
            return false;
        }

        // Copy buffer.
        final int encBufferSize = packetHandler.getEncryptedBuffer().size();
        if (encBufferSize < ShockwaveProperBuffer.PACKET_SIZE_MIN_ENCRYPTED) {
            return false;
        }

        final byte[] encBuffer = new byte[encBufferSize];
        for (int i = 0; i < encBufferSize; i++) {
            encBuffer[i] = packetHandler.getEncryptedBuffer().get(i);
        }

        if (packetHandler instanceof FlashPacketHandler) {
            // Fast-path.
            for (byte[] possible : potentialRC4tables) {
                if (bruteFlashFast(packetHandler, encBuffer, possible)) {
                    return true;
                }
            }

            // Slow-path.
            for (byte[] possible : potentialRC4tables) {
                if (bruteFlashSlow(packetHandler, encBuffer, possible)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean bruteFlashFast(EncryptedPacketHandler packetHandler, byte[] encBuffer, byte[] tableState) {
        final int EstimatedQ = encBuffer.length % 256;

        for (int j = 0; j < 256; j++) {
            if (bruteFlash(packetHandler, encBuffer, tableState, EstimatedQ, j)) {
                logger.debug("Brute forced flash with fast path");
                return true;
            }
        }

        return false;
    }

    private boolean bruteFlashSlow(EncryptedPacketHandler packetHandler, byte[] encBuffer, byte[] tableState) {
        for (int q = 0; q < 256; q++) {
            for (int j = 0; j < 256; j++) {
                if (bruteFlash(packetHandler, encBuffer, tableState, q, j)) {
                    logger.debug("Brute forced flash with slow path");
                    return true;
                }
            }
        }

        return false;
    }

    private boolean bruteFlash(EncryptedPacketHandler flashPacketHandler, byte[] encBuffer, byte[] tableState, int q, int j) {
        final byte[] keycpy = Arrays.copyOf(tableState, tableState.length);
        final RC4 rc4Tryout = new RC4(keycpy, q, j);

        if (flashPacketHandler.getDirection() == HMessage.Direction.TOSERVER) {
            rc4Tryout.undoRc4(encBuffer);
        }

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
                // ignore
            }
        }

        return false;
    }
}
