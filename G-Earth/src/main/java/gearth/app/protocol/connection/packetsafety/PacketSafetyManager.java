package gearth.app.protocol.connection.packetsafety;

import gearth.app.protocol.HConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PacketSafetyManager {

    public static final PacketSafetyManager PACKET_SAFETY_MANAGER = new PacketSafetyManager();

    private final Map<String, SafePacketsContainer> safePacketContainers = new ConcurrentHashMap<>();

    private PacketSafetyManager() {

    }

    public void initialize(HConnection connection) {
        connection.addTrafficListener(0, message -> {
            String hotelVersion = connection.getHotelVersion();
            if (hotelVersion.equals("")) return;

            SafePacketsContainer safePacketsContainer = getPacketContainer(hotelVersion);
            safePacketsContainer.validateSafePacket(message.getPacket().headerId(), message.getDestination());
        });
    }

    public SafePacketsContainer getPacketContainer(String hotelVersion) {
        return safePacketContainers.computeIfAbsent(hotelVersion, v -> new SafePacketsContainer());
    }

}
