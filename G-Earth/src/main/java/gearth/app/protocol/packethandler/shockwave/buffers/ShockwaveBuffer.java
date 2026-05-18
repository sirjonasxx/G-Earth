package gearth.app.protocol.packethandler.shockwave.buffers;

import gearth.app.protocol.packethandler.PayloadBuffer;

import java.util.ArrayList;
import java.util.Arrays;

public class ShockwaveBuffer extends PayloadBuffer {

    @Override
    public byte[][] receive() {
        if (buffer.length < 3) {
            return new byte[0][];
        }

        // Incoming packets are delimited by chr(1).
        // We need to split the buffer by chr(1) and then parse each packet.
        final ArrayList<byte[]> packets = new ArrayList<>();

        int curPos = 0;

        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == 1) {
                packets.add(Arrays.copyOfRange(buffer, curPos, i));
                curPos = i + 1;
            }
        }

        buffer = Arrays.copyOfRange(buffer, curPos, buffer.length);

        return packets.toArray(new byte[0][]);
    }

}
