package gearth.protocol.packethandler.shockwave.buffers;

import gearth.protocol.HPacket;
import gearth.protocol.format.shockwave.ShockPacket;
import gearth.protocol.packethandler.ByteArrayUtils;

import java.util.ArrayList;

public class ShockwaveOutBuffer implements ShockwaveBuffer {

    private byte[] buffer = new byte[0];

    @Override
    public void push(byte[] data) {
        buffer = buffer.length == 0 ? data.clone() : ByteArrayUtils.combineByteArrays(buffer, data);
    }

    @Override
    public ShockPacket[] receive() {
        if (buffer.length < 5) {
            return new ShockPacket[0];
        }

        ArrayList<HPacket> all = new ArrayList<>();

        return all.toArray(new ShockPacket[all.size()]);
    }
}
