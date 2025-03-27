package gearth.services.nitro.hotels;

import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.services.nitro.NitroHotel;
import gearth.services.nitro.NitroPacketModifier;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;

/**
 * Created by Mikee on 2025-03-27.
 */
public class HabboCity extends NitroHotel {

    public HabboCity() {
        super("habbocity.me", Collections.singletonList("wss://websocket.habbocity.me/websocket/"));
    }

    @Override
    public NitroPacketModifier createPacketModifier() {
        return new HabboCityPacketModifier();
    }

    public static class HabboCityPacketModifier implements NitroPacketModifier {

        private static final byte[] StaticKey = Hex.decode("DBEB792BAB40EB810974E53B239F69EA");
        private static final byte[] StaticIv = Hex.decode("3E3C0D6AC569034BF7AD9D13");

        private final Cipher staticCipherEnc;
        private final Cipher staticCipherDec;

        private Cipher clientCipherEnc;
        private Cipher clientCipherDec;

        private Cipher serverCipherEnc;
        private Cipher serverCipherDec;

        private boolean firstClient = true;
        private boolean firstServer = true;

        public HabboCityPacketModifier() {
            try {
                final SecretKeySpec staticKeySpec = new SecretKeySpec(StaticKey, "AES");
                final GCMParameterSpec staticParameterSpec = new GCMParameterSpec(128, StaticIv);

                this.staticCipherEnc = Cipher.getInstance("AES/GCM/NoPadding", "BC");
                this.staticCipherEnc.init(Cipher.ENCRYPT_MODE, staticKeySpec, staticParameterSpec);

                this.staticCipherDec = Cipher.getInstance("AES/GCM/NoPadding", "BC");
                this.staticCipherDec.init(Cipher.DECRYPT_MODE, staticKeySpec, staticParameterSpec);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void initCiphers(final byte[] handshakePacket) throws Exception {
            // Read SSO from packet.
            final HPacket packet = HPacketFormat.EVA_WIRE.createPacket(handshakePacket);
            final String sso = packet.readString();

            // Hash for keys.
            // https://www.youtube.com/watch?v=dQw4w9WgXcQ
            final byte[] aesKey = this.sha256(sso + "Never gonna give you up 🤡");
            final byte[] aesIv = this.sha256(sso + "Never gonna let you down 🤡");

            // Setup keys.
            final SecretKeySpec aesKeySpec = new SecretKeySpec(aesKey, 0, 16, "AES");
            final IvParameterSpec aesIvSpec = new IvParameterSpec(aesIv, 0, 16);

            // Setup ciphers.
            this.clientCipherEnc = Cipher.getInstance("AES/CTR/NoPadding");
            this.clientCipherEnc.init(Cipher.ENCRYPT_MODE, aesKeySpec, aesIvSpec);

            this.clientCipherDec = Cipher.getInstance("AES/CTR/NoPadding");
            this.clientCipherDec.init(Cipher.DECRYPT_MODE, aesKeySpec, aesIvSpec);

            this.serverCipherEnc = Cipher.getInstance("AES/CTR/NoPadding");
            this.serverCipherEnc.init(Cipher.ENCRYPT_MODE, aesKeySpec, aesIvSpec);

            this.serverCipherDec = Cipher.getInstance("AES/CTR/NoPadding");
            this.serverCipherDec.init(Cipher.DECRYPT_MODE, aesKeySpec, aesIvSpec);
        }

        private byte[] sha256(final String data) throws Exception {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");
            final byte[] input = data.getBytes(StandardCharsets.UTF_8);

            return digest.digest(input);
        }

        private byte[] doCipher(Cipher cipher, byte[] data) {
            if (cipher == null) {
                throw new IllegalStateException("Cipher not initialized.");
            }

            final byte[] modified = cipher.update(data);

            if (modified == null) {
                throw new IllegalStateException("Failed to use cipher on packet. " +
                        "This can happen if the BouncyCastyle security provider is used for AES/CTR. " +
                        "Please use the default security provider instead of " + cipher.getProvider().getName() + ".");
            }

            return modified;
        }

        @Override
        public byte[] clientToGearth(byte[] data) throws Exception {
            if (this.firstClient) {
                this.firstClient = false;

                data = this.staticCipherDec.doFinal(data);

                this.initCiphers(data);
            } else {
                data = doCipher(this.clientCipherDec, data);
            }

            return data;
        }

        @Override
        public byte[] gearthToClient(byte[] data) throws Exception {
            return doCipher(this.clientCipherEnc, data);
        }

        @Override
        public byte[] serverToGearth(byte[] data) {
            return doCipher(this.serverCipherDec, data);
        }

        @Override
        public byte[] gearthToServer(byte[] data) throws Exception {
            if (this.firstServer) {
                this.firstServer = false;

                data = this.staticCipherEnc.doFinal(data);
            } else {
                data = doCipher(this.serverCipherEnc, data);
            }

            return data;
        }
    }
}
