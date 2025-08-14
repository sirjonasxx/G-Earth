package gearth.services.packet_representation;

import gearth.protocol.HPacket;

public interface PacketReplaceWriter {

    void write(HPacket temp, String value);

}
