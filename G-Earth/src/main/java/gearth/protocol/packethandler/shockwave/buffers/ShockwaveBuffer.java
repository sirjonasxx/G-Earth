package gearth.protocol.packethandler.shockwave.buffers;

import gearth.protocol.HPacket;

public interface ShockwaveBuffer {

    void push(byte[] data);

    HPacket[] receive();

}
