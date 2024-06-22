package gearth.protocol.packethandler.shockwave.packets;

import gearth.encoding.VL64Encoding;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import org.apache.commons.lang3.ArrayUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// Server to Client
public class ShockPacketIncoming extends ShockPacket {
    public ShockPacketIncoming(byte[] packet) {
        super(HPacketFormat.WEDGIE_INCOMING, packet);
    }

    public ShockPacketIncoming(HPacket packet) {
        super(HPacketFormat.WEDGIE_INCOMING, packet);
    }

    public ShockPacketIncoming(String packet) {
        super(HPacketFormat.WEDGIE_INCOMING, packet);
    }

    public ShockPacketIncoming(int header) {
        super(HPacketFormat.WEDGIE_INCOMING, header);
    }

    @Override
    public HPacket appendBoolean(boolean value) {
        isEdited = true;
        appendBytes(VL64Encoding.encode(value ? 1 : 0));
        return this;
    }

    @Override
    public HPacket appendShort(short value) {
        return appendUShort(value);
    }

    @Override
    public HPacket appendUShort(int value) {
        isEdited = true;
        appendBytes(VL64Encoding.encode(value));
        return this;
    }

    @Override
    public HPacket appendInt(int value) {
        isEdited = true;
        appendBytes(VL64Encoding.encode(value));
        return this;
    }

    @Override
    public HPacket appendString(String value) {
        return appendString(value, StandardCharsets.ISO_8859_1);
    }

    @Override
    public HPacket appendString(String value, Charset charset) {
        isEdited = true;

        final byte[] data = value.getBytes(charset);
        appendBytes(data);
        appendByte((byte) 2);
        return this;
    }

    @Override
    public boolean readBoolean() {
        return readInteger() == 1;
    }

    @Override
    public short readShort() {
        return (short) readInteger();
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
    public String readString() {
        return this.readString(StandardCharsets.UTF_8);
    }

    @Override
    public String readString(Charset charset) {
        String result;

        int startPos = readIndex;
        int endPos = ArrayUtils.indexOf(packetInBytes, (byte) 2, startPos);
        if (endPos > 0) {
            result = new String(Arrays.copyOfRange(packetInBytes, startPos, endPos), charset);
        } else {
            result = "";
        }

        readIndex = endPos + 1;

        return result;
    }

    @Override
    public HPacket copy() {
        return new ShockPacketIncoming(this);
    }
}
