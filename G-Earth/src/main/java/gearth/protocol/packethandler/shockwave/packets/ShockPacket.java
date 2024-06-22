package gearth.protocol.packethandler.shockwave.packets;

import gearth.encoding.Base64Encoding;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.services.packet_info.PacketInfoManager;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;

public abstract class ShockPacket extends HPacket {
    public ShockPacket(HPacketFormat format, byte[] packet) {
        super(packet);
        packetFormat = format;
        readIndex = 2;
    }

    public ShockPacket(HPacketFormat format, HPacket packet) {
        super(packet);
        packetFormat = format;
        readIndex = 2;
    }

    public ShockPacket(HPacketFormat format, String packet) {
        super(packet, format);
        packetFormat = format;
        readIndex = 2;
    }

    public ShockPacket(HPacketFormat format, int header) {
        super(Base64Encoding.encode(header, 2));
        packetFormat = format;
        readIndex = 2;
    }

    @Override
    public HPacket appendBoolean(boolean b) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendByte(byte b) {
        return super.appendByte(b);
    }

    @Override
    public HPacket appendBytes(byte[] bytes) {
        return super.appendBytes(bytes);
    }

    @Override
    public HPacket appendInt(int i) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendUShort(int ushort) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendShort(short s) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendLong(long l) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendDouble(double d) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendFloat(float f) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendString(String s, Charset charset) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendString(String s) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendLongString(String s, Charset charset) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendLongString(String s) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendObjects(Object... objects) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public HPacket appendObject(Object o) throws InvalidParameterException {
        throw new ShockPacketUnsupported();
    }

    @Override
    public byte readByte() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public byte readByte(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public short readShort() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public short readShort(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public int readUshort() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public int readUshort(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public int readInteger() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public int readInteger(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public double readDouble() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public double readDouble(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public float readFloat() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public float readFloat(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public byte[] readBytes(int length) {
        return super.readBytes(length);
    }

    @Override
    public byte[] readBytes(int length, int index) {
        return super.readBytes(length, index);
    }

    @Override
    public long readLong() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public long readLong(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public String readString(Charset charset) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public String readString(int index, Charset charset) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public String readString() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public String readString(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public String readLongString(Charset charset) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public String readLongString(int index, Charset charset) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public String readLongString() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public String readLongString(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    public boolean readBoolean() {
        throw new ShockPacketUnsupported();
    }

    @Override
    public boolean readBoolean(int index) {
        throw new ShockPacketUnsupported();
    }

    @Override
    protected void replacePacketId(short headerId) {
        final byte[] header = Base64Encoding.encode(headerId, 2);

        this.packetInBytes[0] = header[0];
        this.packetInBytes[1] = header[1];
    }

    @Override
    public int headerId() {
        final String header = new String(this.readBytes(2, 0), StandardCharsets.ISO_8859_1);
        final int headerId = Base64Encoding.decode(header.getBytes());

        return headerId;
    }

    @Override
    public int length() {
        return this.packetInBytes.length;
    }

    @Override
    public void resetReadIndex() {
        setReadIndex(2);
    }

    @Override
    public boolean isCorrupted() {
        return packetInBytes.length < 2;
    }

    @Override
    public String toExpression(HMessage.Direction direction, PacketInfoManager packetInfoManager, boolean removeShuffle) {
        return ""; // Unsupported
    }

    @Override
    public String toExpression(PacketInfoManager packetInfoManager, boolean removeShuffle) {
        return ""; // Unsupported
    }

    @Override
    public String toExpression() {
        return ""; // Unsupported
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public void fixLength() {
        // no-op
        // length not needed for Shockwave packets
    }
}
