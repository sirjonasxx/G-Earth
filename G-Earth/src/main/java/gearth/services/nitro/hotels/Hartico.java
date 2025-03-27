package gearth.services.nitro.hotels;

import gearth.services.nitro.NitroHotel;
import gearth.services.nitro.NitroPacketModifier;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Collections;

/**
 * Created by Mikee on 2025-03-27.
 */
public class Hartico extends NitroHotel {

    public Hartico() {
        super("hartico.tv", Collections.singletonList("wss://server.hartico.tv/"));
    }

    @Override
    public NitroPacketModifier createPacketModifier() {
        return new HarticoPacketModifier();
    }

    public static class HarticoPacketModifier implements NitroPacketModifier {

        private static final SecretKeySpec StaticKey = new SecretKeySpec(Base64.decode("eNYjKGxfQsmtdpuLHuznYA=="), "AES");
        private static final SecureRandom Random = new SecureRandom();

        private byte[] doEncrypt(byte[] data) throws Exception {
            // Generate iv.
            final byte[] iv = new byte[12];
            Random.nextBytes(iv);

            // Init cipher.
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");

            cipher.init(Cipher.ENCRYPT_MODE, StaticKey, new GCMParameterSpec(128, iv));

            // Encrypt data.
            final byte[] encrypted = new byte[iv.length + cipher.getOutputSize(data.length)];

            cipher.doFinal(data, 0, data.length, encrypted, iv.length);

            // Copy iv to the start.
            System.arraycopy(iv, 0, encrypted, 0, iv.length);

            return encrypted;
        }

        private byte[] doDecrypt(byte[] data) throws Exception {
            final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");

            cipher.init(Cipher.DECRYPT_MODE, StaticKey, new GCMParameterSpec(128, data, 0, 12));

            return cipher.doFinal(data, 12, data.length - 12);
        }

        @Override
        public byte[] clientToGearth(byte[] data) throws Exception {
            return doDecrypt(data);
        }

        @Override
        public byte[] gearthToClient(byte[] data) {
            return data;
        }

        @Override
        public byte[] serverToGearth(byte[] data) {
            return data;
        }

        @Override
        public byte[] gearthToServer(byte[] data) throws Exception {
            return doEncrypt(data);
        }
    }
}
