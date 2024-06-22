package gearth.protocol.packethandler.shockwave.buffers;

import gearth.encoding.Base64Encoding;
import gearth.protocol.HPacket;
import gearth.protocol.packethandler.ByteArrayUtils;
import gearth.protocol.packethandler.shockwave.packets.ShockPacket;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketOutgoing;

import java.util.ArrayList;
import java.util.Arrays;

public class ShockwaveOutBuffer implements ShockwaveBuffer {

    private static final int PACKET_LENGTH_SIZE = 3;
    private static final int PACKET_HEADER_SIZE = 2;

    private static final int PACKET_SIZE_MIN = PACKET_LENGTH_SIZE + PACKET_HEADER_SIZE;

    private byte[] buffer = new byte[0];

    @Override
    public void push(byte[] data) {
        buffer = buffer.length == 0 ? data.clone() : ByteArrayUtils.combineByteArrays(buffer, data);
    }

    @Override
    public HPacket[] receive() {
        if (buffer.length < PACKET_SIZE_MIN) {
            return new ShockPacket[0];
        }

        ArrayList<ShockPacket> out = new ArrayList<>();

        while (buffer.length >= PACKET_SIZE_MIN) {
            int length = Base64Encoding.decode(new byte[]{buffer[0], buffer[1], buffer[2]});
            if (buffer.length < length + PACKET_LENGTH_SIZE) {
                break;
            }

            int endPos = length + PACKET_LENGTH_SIZE;
            byte[] packet = Arrays.copyOfRange(buffer, PACKET_LENGTH_SIZE, endPos);

            out.add(new ShockPacketOutgoing(packet));

            buffer = Arrays.copyOfRange(buffer, endPos, buffer.length);
        }

        return out.toArray(new ShockPacket[0]);
    }
}
