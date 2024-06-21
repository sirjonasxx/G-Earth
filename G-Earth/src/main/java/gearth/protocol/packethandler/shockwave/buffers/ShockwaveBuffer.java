package gearth.protocol.packethandler.shockwave.buffers;

import gearth.protocol.format.shockwave.ShockPacket;

public interface ShockwaveBuffer {

    void push(byte[] data);

    ShockPacket[] receive();

}
