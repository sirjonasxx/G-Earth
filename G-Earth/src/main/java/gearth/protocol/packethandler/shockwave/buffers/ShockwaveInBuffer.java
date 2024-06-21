package gearth.protocol.packethandler.shockwave.buffers;

import gearth.protocol.HPacket;
import gearth.protocol.packethandler.ByteArrayUtils;
import gearth.protocol.packethandler.shockwave.packets.ShockPacket;

import java.util.ArrayList;
import java.util.Arrays;

public class ShockwaveInBuffer implements ShockwaveBuffer {

    private byte[] buffer = new byte[0];

    @Override
    public void push(byte[] data) {
        buffer = buffer.length == 0 ? data.clone() : ByteArrayUtils.combineByteArrays(buffer, data);
    }

    @Override
    public HPacket[] receive() {
        if (buffer.length < 3) {
            return new ShockPacket[0];
        }

        // Incoming packets are delimited by chr(1).
        // We need to split the buffer by chr(1) and then parse each packet.
        ArrayList<ShockPacket> packets = new ArrayList<>();

        int curPos = 0;

        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == 1) {
                byte[] packetData = Arrays.copyOfRange(buffer, curPos, i);
                packets.add(new ShockPacket(packetData));
                curPos = i + 1;
            }
        }

        buffer = Arrays.copyOfRange(buffer, curPos, buffer.length);

        return packets.toArray(new ShockPacket[0]);
    }
}
