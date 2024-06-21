package gearth.protocol.packethandler.shockwave.packets;

import gearth.encoding.Base64Encoding;
import gearth.encoding.VL64Encoding;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// Server to Client
public class ShockPacketIncoming extends ShockPacket {
    public ShockPacketIncoming(byte[] packet) {
        super(packet);
    }

    public ShockPacketIncoming(HPacket packet) {
        super(packet);
    }

    public ShockPacketIncoming(String packet) {
        super(packet, HPacketFormat.WEDGIE_INCOMING);
    }

    public ShockPacketIncoming(int header) {
        super(header);
    }

    @Override
    public HPacket appendBoolean(boolean b) {
        isEdited = true;
        appendBytes(VL64Encoding.encode(b ? 1 : 0));
        return this;
    }

    @Override
    public HPacket appendUShort(int ushort) {
        isEdited = true;
        appendBytes(VL64Encoding.encode(ushort));
        return this;
    }

    @Override
    public HPacket appendInt(int value) {
        isEdited = true;
        appendBytes(VL64Encoding.encode(value));
        return this;
    }

    @Override
    public HPacket appendString(String s) {
        return appendString(s, StandardCharsets.ISO_8859_1);
    }

    @Override
    public HPacket appendString(String s, Charset charset) {
        isEdited = true;

        final byte[] data = s.getBytes(charset);
        appendBytes(data);
        appendByte((byte) 2);
        return this;
    }
}
