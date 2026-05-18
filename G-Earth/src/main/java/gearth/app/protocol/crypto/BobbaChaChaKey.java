package gearth.app.protocol.crypto;

import javax.crypto.Cipher;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BobbaChaChaKey {

    private final byte[] key;
    private final byte[] nonce;

    private long counter;
    private Cipher cipher;

    public BobbaChaChaKey(byte[] key, byte[] nonce) {
        this.key = key;
        this.nonce = nonce;
        this.counter = 0;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public byte[] getNextNonce() {
        final long current = counter++;
        final byte[] next = nonce.clone();

        ByteBuffer buf = ByteBuffer.wrap(next, 4, 8).order(ByteOrder.LITTLE_ENDIAN);
        long base = buf.getLong();
        buf.position(4);
        buf.putLong(base + current);

        return next;
    }
}
