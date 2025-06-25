package gearth.protocol.connection.packetsafety;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.protocol.connection.HClient;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketIncoming;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketOutgoing;

public class PacketTypeChecker {
    /**
     * Ensure that the packet is of the correct type for the client
     *
     * @param clientType The client we are sending to.
     * @param direction The direction of the packet.
     * @param packet The packet to check.
     * @throws PacketTypeException If the packet is not of the correct type.
     */
    public static void ensureValid(HClient clientType, HMessage.Direction direction, HPacket packet) {
        if (clientType == HClient.SHOCKWAVE) {
            if (direction == HMessage.Direction.TOSERVER && !(packet instanceof ShockPacketOutgoing)) {
                throw new PacketTypeException(String.format("ShockPacketOutgoing expected for %s", clientType));
            }

            if (direction == HMessage.Direction.TOCLIENT && !(packet instanceof ShockPacketIncoming)) {
                throw new PacketTypeException(String.format("ShockPacketIncoming expected for %s", clientType));
            }
        } else if (packet.getFormat() != HPacketFormat.EVA_WIRE) {
            throw new PacketTypeException(String.format("Invalid packet, expected %s for client type %s, got %s", HPacketFormat.EVA_WIRE, clientType, packet.getFormat()));
        }
    }
}
