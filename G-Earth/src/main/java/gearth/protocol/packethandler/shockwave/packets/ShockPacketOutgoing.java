package gearth.protocol.packethandler.shockwave.packets;

import gearth.encoding.Base64Encoding;
import gearth.encoding.VL64Encoding;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// Client to Server
public class ShockPacketOutgoing extends ShockPacket {
    public ShockPacketOutgoing(byte[] packet) {
        super(packet);
    }

    public ShockPacketOutgoing(HPacket packet) {
        super(packet);
    }

    public ShockPacketOutgoing(String packet) {
        super(packet, HPacketFormat.WEDGIE_OUTGOING);
    }

    public ShockPacketOutgoing(int header) {
        super(header);
    }

    @Override
    public HPacket appendBoolean(boolean b) {
        return appendInt(b ? 1 : 0);
    }

    @Override
    public HPacket appendUShort(int ushort) {
        isEdited = true;
        appendBytes(Base64Encoding.encode(ushort, 2));
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
        appendUShort(data.length);
        appendBytes(data);
        return this;
    }
}
