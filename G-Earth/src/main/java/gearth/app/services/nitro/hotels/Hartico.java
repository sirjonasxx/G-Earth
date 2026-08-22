package gearth.app.services.nitro.hotels;

import gearth.app.services.nitro.NitroAsset;
import gearth.app.services.nitro.NitroHotel;
import gearth.app.services.nitro.NitroPacketModifier;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Mikee on 2025-03-27.
 */
public class Hartico extends NitroHotel {

    private byte[] tripleDesKey;

    public Hartico() {
        super("hartico.tv",
                Collections.singletonList("wss://server.hartico.tv/"),
                Collections.singletonList(new NitroAsset("images.hartico.tv", "/cosmos/img/logos/logo.png")));
    }

    @Override
    public NitroPacketModifier createPacketModifier() {
        if (tripleDesKey == null) {
            throw new IllegalStateException("Failed to create packet modifier, keys not initialized");
        }

        return new HarticoPacketModifier(tripleDesKey);
    }

    @Override
    protected void loadAsset(String host, String uri, byte[] data) {
        if ("/cosmos/img/logos/logo.png".equals(uri)) {
            tripleDesKey = extractKeyFromImage(data);
        }
    }

    private static byte[] extractKeyFromImage(final byte[] imageData) {
        // Load data as Image
        final Image image = new Image(new ByteArrayInputStream(imageData));
        final byte[] pixelData = new byte[(int) (image.getWidth() * image.getHeight() * 4)];

        image.getPixelReader().getPixels(0, 0, (int) image.getWidth(), (int) image.getHeight(), PixelFormat.getByteBgraInstance(), pixelData, 0, (int) image.getWidth() * 4);

        // Convert from BGRA to RGBA
        for (int i = 0; i < pixelData.length; i += 4) {
            byte b = pixelData[i];
            byte g = pixelData[i + 1];
            byte r = pixelData[i + 2];
            byte a = pixelData[i + 3];

            pixelData[i]     = r;
            pixelData[i + 1] = g;
            pixelData[i + 2] = b;
            pixelData[i + 3] = a;
        }

        // Extract 128 pixels.
        final byte[] colorBits = new byte[128];

        for (int i = 0; i < colorBits.length; i++) {
            colorBits[i] = (byte) (pixelData[(i * 4) + 1] & 1);
        }

        // Group bits into bytes.
        final byte[] colorBytes = new byte[16];

        for (int i = 0; i < 16; i++) {
            int value = 0;
            for (int j = 0; j < 8; j++) {
                value = (value << 1) | colorBits[i * 8 + j];
            }
            colorBytes[i] = (byte) value;
        }

        final byte[] colorBytesExtended = new byte[24];

        System.arraycopy(colorBytes, 0, colorBytesExtended, 0, 16);
        System.arraycopy(colorBytes, 0, colorBytesExtended, 16, 8);

        return colorBytesExtended;
    }

    public static class HarticoPacketModifier implements NitroPacketModifier {

        private final SecretKeySpec secretKeySpec;

        private boolean isAuthenticated;

        public HarticoPacketModifier(byte[] tripleDesKey) {
            secretKeySpec = new SecretKeySpec(tripleDesKey, "TripleDES");
        }

        private byte[] encrypt(final byte[] data) throws GeneralSecurityException {
            final byte[] iv = new byte[8];
            ThreadLocalRandom.current().nextBytes(iv);

            final Cipher cipher = Cipher.getInstance("TripleDES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv, 0, 8));
            final byte[] encrypted = cipher.doFinal(data);
            final byte[] result = new byte[iv.length + encrypted.length];

            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

            return result;
        }

        private byte[] decrypt(final byte[] data) throws GeneralSecurityException {
            final Cipher cipher = Cipher.getInstance("TripleDES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(data, 0, 8));
            return cipher.doFinal(data, 8, data.length - 8);
        }

        @Override
        public byte[] clientToGearth(byte[] data) throws GeneralSecurityException {
            if (isAuthenticated) {
                data = decrypt(data);
            }

            return data;
        }

        @Override
        public byte[] gearthToClient(byte[] data) {
            if (!isAuthenticated &&
                    data.length == 6 &&
                    data[0] == 0x00 &&
                    data[1] == 0x00 &&
                    data[2] == 0x00 &&
                    data[3] == 0x02 &&
                    data[4] == 0x09 &&
                    data[5] == -69) {
                isAuthenticated = true;
            }

            return data;
        }

        @Override
        public byte[] serverToGearth(byte[] data) {
            return data;
        }

        @Override
        public byte[] gearthToServer(byte[] data)  throws GeneralSecurityException {
            if (isAuthenticated) {
                data = encrypt(data);
            }

            return data;
        }
    }
}
