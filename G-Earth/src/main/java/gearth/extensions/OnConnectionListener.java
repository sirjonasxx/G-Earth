package gearth.extensions;

import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.connection.HClient;

public interface OnConnectionListener {
    void onConnection(String host, int port, String hotelversion, String clientIdentifier, HClient clientType, PacketInfoManager packetInfoManager);
}
