package gearth.protocol.packethandler;

import gearth.protocol.crypto.RC4Cipher;

public abstract class PayloadBuffer {

    protected byte[] buffer;

    public PayloadBuffer() {
        this.buffer = new byte[0];
    }

    /**
     * Make sure to call deepCopy on the cipher if you use it.
     */
    public abstract void setCipher(RC4Cipher cipher);

    public void push(byte[] data) {
        buffer = buffer.length == 0 ? data.clone() : ByteArrayUtils.combineByteArrays(buffer, data);
    }

    public abstract byte[][] receive();

    public boolean isEmpty() {
        return buffer.length == 0;
    }

}
