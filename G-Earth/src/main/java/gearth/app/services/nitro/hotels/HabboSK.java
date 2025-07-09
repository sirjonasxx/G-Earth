package gearth.app.services.nitro.hotels;

import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.app.protocol.crypto.RC4;
import gearth.app.services.nitro.NitroHotel;
import gearth.app.services.nitro.NitroPacketModifier;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

/**
 * Created by Mikee on 2025-05-21.
 */
public class HabboSK extends NitroHotel {

    private static final Logger LOG = LoggerFactory.getLogger(HabboSK.class);

    public HabboSK() {
        super("habbosk.us",
                Collections.singletonList("wss://ws.habbosk.us/ws"),
                Collections.emptyList());
    }

    @Override
    public NitroPacketModifier createPacketModifier() {
        return new HabboSKPacketModifier();
    }

    @Override
    protected void loadAsset(String host, String uri, byte[] data) {

    }

    public static class HabboSKPacketModifier implements NitroPacketModifier {

        private static final String DEFAULT_KEY = "253146253045402535442530436e25304152752531376342502530452d5f5169253542253136253144253142253742595825303342612531426655253146402532332531305357574a6f253131253141253139722535442531376d25303152362532336c5a6d58253038374225314425313342514d582530446252253031754e68472530302535452537425f2530416d544a4f2531397525354225304654253130664d6d51253144253142724d25303250502531432533422531344b4d73592531376d253032573476253345253545612535425862452531344025314455253132253046253036672530322530342537454b25334542562530432d253034253041672530462531452531444e732530442535455725313025363025313867253037253130253130742531452530352530322530384b6f253135253141492532302535434368565a322532326e25304625334325303525303937253130482531342531442530332531302530385062253036542f4963435025304671535f6a2530464e25314549752535432535435325314133253138622530372531302531422532364a2530302530322530362531386e253130253139253144253235253038496c2530435169776c2530426c25354325354330253136253139253135253145554d5a25304364253036557a2531432533452531352530355f253744253545";

        private RC4 C2S_RC4_IN;
        private RC4 C2S_RC4_OUT;

        private RC4 S2C_RC4_IN;
        private RC4 S2C_RC4_OUT;

        private byte[] clientRandomKey;
        private byte[] serverRandomKey;

        public HabboSKPacketModifier() {
            this.C2S_RC4_IN = new RC4(Hex.decode(DEFAULT_KEY));
            this.C2S_RC4_OUT = new RC4(Hex.decode(DEFAULT_KEY));
            this.S2C_RC4_IN = null;
            this.S2C_RC4_OUT = null;
            this.clientRandomKey = null;
            this.serverRandomKey = null;
        }

        private void readClientRandomKey(final byte[] data) {
            final HPacket packet = HPacketFormat.EVA_WIRE.createPacket(data);

            if (packet.headerId() != 3792) {
                return;
            }

            LOG.debug("HabboSK readClientRandomKey");

            this.clientRandomKey = packet.readString().getBytes(StandardCharsets.US_ASCII);
        }

        private void readServerRandomKey(final byte[] data) throws NoSuchAlgorithmException {
            final HPacket packet = HPacketFormat.EVA_WIRE.createPacket(data);

            if (packet.headerId() != 9384) {
                return;
            }

            LOG.debug("HabboSK readServerRandomKey");

            this.serverRandomKey = packet.readString().getBytes(StandardCharsets.US_ASCII);
            this.initCiphers();
        }

        private void initCiphers() throws NoSuchAlgorithmException {
            if (this.clientRandomKey == null || this.serverRandomKey == null) {
                throw new IllegalStateException("Failed to init ciphers, keys not initialized");
            }

            // Combine keys
            final byte[] key = new byte[this.clientRandomKey.length + this.serverRandomKey.length];

            System.arraycopy(this.clientRandomKey, 0, key, 0, this.clientRandomKey.length);
            System.arraycopy(this.serverRandomKey, 0, key, this.clientRandomKey.length, this.serverRandomKey.length);

            // Retrieve MD5 of key.
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key);
            final byte[] digest = md.digest();

            // Retrieve keys.
            final byte[] s2cKey = key;
            final byte[] c2sKey = Hex.encode(digest);

            LOG.debug("s2cKey {}", new String(s2cKey));
            LOG.debug("c2sKey {}", new String(c2sKey));

            // Normal
            this.S2C_RC4_IN = new RC4(s2cKey);
            this.S2C_RC4_OUT = new RC4(s2cKey);

            // MD5
            this.C2S_RC4_IN = new RC4(c2sKey);
            this.C2S_RC4_OUT = new RC4(c2sKey);
        }

        @Override
        public byte[] clientToGearth(byte[] data) {
            final byte[] plain = C2S_RC4_IN.cipher(data);

            if (this.clientRandomKey == null) {
                this.readClientRandomKey(plain);
            }

            return plain;
        }

        @Override
        public byte[] gearthToClient(byte[] data) throws NoSuchAlgorithmException {
            if (this.S2C_RC4_OUT != null) {
                data = S2C_RC4_OUT.cipher(data);
            } else if (this.serverRandomKey == null) {
                this.readServerRandomKey(data);
            }

            return data;
        }

        @Override
        public byte[] serverToGearth(byte[] data) {
            if (this.S2C_RC4_IN != null) {
                data = S2C_RC4_IN.cipher(data);
            }

            return data;
        }

        @Override
        public byte[] gearthToServer(byte[] data) {
            return C2S_RC4_OUT.cipher(data);
        }
    }
}
