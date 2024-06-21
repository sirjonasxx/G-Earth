package gearth.protocol.packethandler.shockwave.packets;

import gearth.encoding.Base64Encoding;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;

public class ShockPacket extends HPacket {
    public ShockPacket(byte[] packet) {
        super(packet);
    }

    public ShockPacket(HPacket packet) {
        super(packet);
    }

    public ShockPacket(String packet) {
        super(packet);
    }

    public ShockPacket(int header) {
        super(header);
    }

    public ShockPacket(int header, byte[] bytes) {
        super(header, bytes);
    }

    public ShockPacket(int header, Object... objects) throws InvalidParameterException {
        super(header, objects);
    }

    public ShockPacket(String identifier, HMessage.Direction direction) throws InvalidParameterException {
        super(identifier, direction);
    }

    public ShockPacket(String identifier, HMessage.Direction direction, Object... objects) throws InvalidParameterException {
        super(identifier, direction, objects);
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
    public HPacket copy() {
        return new ShockPacket(this);
    }
}
