package gearth.protocol.packethandler.shockwave.packets;

import gearth.encoding.Base64Encoding;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.packet_info.PacketInfoManager;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ShockPacket extends HPacket {
    public ShockPacket(byte[] packet) {
        super(packet);
        readIndex = 2;
    }

    public ShockPacket(HPacket packet) {
        super(packet);
        readIndex = 2;
    }

    public ShockPacket(String packet) {
        super(packet);
        readIndex = 2;
    }

    public ShockPacket(int header) {
        super(Base64Encoding.encode(header, 2));
        readIndex = 2;
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
    public HPacket appendUShort(int ushort) {
        isEdited = true;
        appendBytes(Base64Encoding.encode(ushort, 2));
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

    @Override
    public HPacket copy() {
        return new ShockPacket(this);
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
