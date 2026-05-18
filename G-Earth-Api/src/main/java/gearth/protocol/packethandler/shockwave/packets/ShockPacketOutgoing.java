package gearth.protocol.packethandler.shockwave.packets;

import gearth.encoding.Base64Encoding;
import gearth.encoding.VL64Encoding;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;

import java.nio.charset.Charset;
import java.security.InvalidParameterException;

// Client to Server
public class ShockPacketOutgoing extends ShockPacket {
    public ShockPacketOutgoing(byte[] packet) {
        super(HPacketFormat.WEDGIE_OUTGOING, packet);
    }

    public ShockPacketOutgoing(HPacket packet) {
        super(HPacketFormat.WEDGIE_OUTGOING, packet);
    }

    public ShockPacketOutgoing(String packet) {
        super(HPacketFormat.WEDGIE_OUTGOING, packet);
    }

    public ShockPacketOutgoing(int header) {
        super(HPacketFormat.WEDGIE_OUTGOING, header);
    }

    public ShockPacketOutgoing(int header, byte[] bytes) {
        super(HPacketFormat.WEDGIE_OUTGOING, header, bytes);
    }

    public ShockPacketOutgoing(int header, Object... objects) throws InvalidParameterException {
        super(HPacketFormat.WEDGIE_OUTGOING, header, objects);
    }

    public ShockPacketOutgoing(String identifier, Object... objects) throws InvalidParameterException {
        super(HPacketFormat.WEDGIE_OUTGOING, identifier, HMessage.Direction.TOSERVER, objects);
    }

    @Override
    public HPacket appendBoolean(boolean value) {
        return appendInt(value ? 1 : 0);
    }

    @Override
    public HPacket appendShort(short value) {
        return appendUShort(value);
    }

    @Override
    public HPacket appendUShort(int value) {
        isEdited = true;
        appendBytes(Base64Encoding.encode(value, 2));
        return this;
    }

    @Override
    public HPacket appendInt(int value) {
        isEdited = true;
        appendBytes(VL64Encoding.encode(value));
        return this;
    }

    @Override
    public HPacket appendString(String value, Charset charset) {
        isEdited = true;

        final byte[] data = value.getBytes(charset);
        appendUShort(data.length);
        appendBytes(data);
        return this;
    }

    @Override
    public boolean readBoolean() {
        return readInteger() == 1;
    }

    @Override
    public short readShort() {
        return (short) Base64Encoding.decode(readBytes(2));
    }

    @Override
    public int readUshort() {
        return this.readShort();
    }

    @Override
    public int readInteger() {
        int length = packetInBytes[readIndex] >> 3 & 7;
        int value = VL64Encoding.decode(packetInBytes, readIndex);

        readIndex += length;

        return value;
    }

    @Override
    public String readString(Charset charset) {
        int length = readUshort();
        byte[] data = readBytes(length);

        return new String(data, charset);
    }

    @Override
    public HPacket copy() {
        return new ShockPacketOutgoing(this);
    }
}
