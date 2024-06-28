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
            TitleBarController.create(alert).showAlert();
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
        if (potentialRC4tables == null || potentialRC4tables.isEmpty()) {
            return false;
        }

        // Copy buffer.
        final int encBufferSize = packetHandler.getEncryptedBuffer().size();
        if (encBufferSize < ShockwaveOutBuffer.PACKET_SIZE_MIN_ENCRYPTED) {
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
        } else if (packetHandler instanceof ShockwavePacketOutgoingHandler) {
            // Fast-path.
            for (byte[] possible : potentialRC4tables) {
                if (bruteShockwaveHeaderFast(packetHandler, encBuffer, possible)) {
                    return true;
                }
            }

            // Slow-path.
            for (byte[] possible : potentialRC4tables) {
                if (bruteShockwaveHeaderSlow(packetHandler, encBuffer, possible)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean bruteShockwaveHeaderFast(EncryptedPacketHandler packetHandler, byte[] encBuffer, byte[] tableState) {
        // Table state Q starts at 152 after premixing.
        // Add 12 for the 3 headers being encrypted and you get 164.
        final int EstimatedQ = 164;

        for (int j = 0; j < 256; j++) {
            if (bruteShockwaveHeader(packetHandler, encBuffer, tableState, EstimatedQ, j)) {
                logger.debug("Brute forced shockwave with fast path");
                return true;
            }
        }

        return false;
    }

    private boolean bruteShockwaveHeaderSlow(EncryptedPacketHandler packetHandler, byte[] encBuffer, byte[] tableState) {
        for (int q = 0; q < 256; q++) {
            for (int j = 0; j < 256; j++) {
                if (bruteShockwaveHeader(packetHandler, encBuffer, tableState, q, j)) {
                    logger.debug("Brute forced shockwave with slow path");
                    return true;
                }
            }
        }

        return false;
    }

    private boolean bruteShockwaveHeader(EncryptedPacketHandler packetHandler, byte[] encBuffer, byte[] tableState, int q, int j) {
        final byte[] tableStateCopy = Arrays.copyOf(tableState, tableState.length);
        final RC4Base64 rc4 = new RC4Base64(tableStateCopy, q, j);

        if (packetHandler.getDirection() == HMessage.Direction.TOSERVER) {
            // Encoded 3 headers, 4 * 3 = 12
            if (!rc4.undoRc4(12)) {
                return false;
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
