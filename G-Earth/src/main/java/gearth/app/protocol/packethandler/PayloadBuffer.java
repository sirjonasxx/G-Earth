package gearth.app.protocol.packethandler;

public abstract class PayloadBuffer {

    protected byte[] buffer;

    public PayloadBuffer() {
        this.buffer = new byte[0];
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void push(byte[] data) {
        buffer = buffer.length == 0 ? data.clone() : ByteArrayUtils.combineByteArrays(buffer, data);
    }

    public abstract byte[][] receive();

    public boolean isEmpty() {
        return buffer.length == 0;
    }

}
