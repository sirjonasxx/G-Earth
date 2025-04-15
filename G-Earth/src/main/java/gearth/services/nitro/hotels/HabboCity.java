package gearth.services.nitro.hotels;

import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.services.nitro.NitroAsset;
import gearth.services.nitro.NitroHotel;
import gearth.services.nitro.NitroPacketModifier;
import gearth.services.nitro.RemoteNitroHelper;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Mikee on 2025-03-27.
 */
public class HabboCity extends NitroHotel {

    private static final Logger log = LoggerFactory.getLogger(HabboCity.class);

    private final RemoteNitroHelper nitroHelper = new RemoteNitroHelper("habbocity.me");
    private byte[] aesKey;
    private byte[] aesIv;
    private List<byte[]> saltKey;
    private List<byte[]> saltIv;

    public HabboCity() {
        super("habbocity.me",
                Collections.singletonList("wss://websocket.habbocity.me/websocket/"),
                Collections.singletonList(new NitroAsset("nitro.habbocity.me", "/crypto/file/city_crypto_bg.wasm")));
    }

    @Override
    public NitroPacketModifier createPacketModifier() {
        if (this.aesKey == null || this.aesIv == null || saltKey == null || saltIv == null) {
            log.error("Failed to create packet modifier, keys not initialized");
            throw new IllegalStateException("Keys not initialized");
        }

        return new HabboCityPacketModifier(this);
    }

    @Override
    protected void loadAsset(String host, String uri, byte[] data) {
        if ("/crypto/file/city_crypto_bg.wasm".equals(uri)) {
            if (!nitroHelper.hasPermission()) {
                nitroHelper.askPermission();

                if (!nitroHelper.hasPermission()) {
                    return;
                }
            }

            final RemoteNitroHelper.HabboCityResponse response = nitroHelper.fetchHabboCity(data);

            if (!"ok".equals(response.getStatus()) || response.getResult() == null) {
                log.error("Failed to fetch proper HabboCity NitroHelper response");
                return;
            }

            this.aesKey = Hex.decode(response.getResult().getAesKey());
            this.aesIv = Hex.decode(response.getResult().getAesIv());
            this.saltKey = new ArrayList<>();
            this.saltIv = new ArrayList<>();

            for (final String x : response.getResult().getSaltKey()) {
                this.saltKey.add(Hex.decode(x));
            }

            for (final String x : response.getResult().getSaltIv()) {
                this.saltIv.add(Hex.decode(x));
            }
        }
    }

    public static class HabboCityPacketModifier implements NitroPacketModifier {

        private final HabboCity parent;

        private final Cipher staticCipherEnc;
        private final Cipher staticCipherDec;

        private Cipher clientCipherEnc;
        private Cipher clientCipherDec;

        private Cipher serverCipherEnc;
        private Cipher serverCipherDec;

        private boolean firstClient = true;
        private boolean firstServer = true;

        public HabboCityPacketModifier(HabboCity parent) {
            this.parent = parent;

            try {
                final SecretKeySpec staticKeySpec = new SecretKeySpec(parent.aesKey, "AES");
                final GCMParameterSpec staticParameterSpec = new GCMParameterSpec(128, parent.aesIv);

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
            final byte[] aesKey = this.sha256(createInput(sso, parent.saltKey));
            final byte[] aesIv = this.sha256(createInput(sso, parent.saltIv));

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

        private List<byte[]> createInput(final String sso, final List<byte[]> salts) throws Exception {
            final byte[] ssoData = sso.getBytes(StandardCharsets.UTF_8);
            final List<byte[]> data = new ArrayList<>();

            if (salts.size() == 1) {
                data.add(ssoData);
                data.add(salts.get(0));
            } else {
                for (int i = 0; i < salts.size(); i++) {
                    data.add(salts.get(i));

                    if (i == salts.size() - 1) {
                        break;
                    }

                    data.add(ssoData);
                }
            }

            return data;
        }

        private byte[] sha256(final List<byte[]> data) throws Exception {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256", "BC");

            int totalLength = 0;

            for (byte[] b : data) {
                totalLength += b.length;
            }


            final ByteBuffer buffer = ByteBuffer.allocate(totalLength);

            for (byte[] b : data) {
                buffer.put(b);
            }

            return digest.digest(buffer.array());
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
        public byte[] gearthToClient(byte[] data) {
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
