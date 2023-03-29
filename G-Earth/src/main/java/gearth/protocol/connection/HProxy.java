package gearth.protocol.connection;

import gearth.protocol.HPacket;
import gearth.protocol.connection.packetsafety.PacketSafetyManager;
import gearth.protocol.connection.packetsafety.SafePacketsContainer;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.packethandler.PacketHandler;

import java.net.ServerSocket;

public class HProxy {

    private final HClient hClient;

    private volatile String input_domain;           //string representation of the domain to intercept
    private volatile String actual_domain;          //dns resolved domain (ignoring hosts file)
    private volatile int actual_port;               //port of the server

    private volatile int intercept_port;            //port used to intercept connection (with the current implementation, must equal actual_port)
    private volatile String intercept_host;         //local ip used to intercept host, example 127.0.0.1

    private volatile ServerSocket proxy_server = null;     //listener for the client

    private volatile PacketHandler inHandler = null;     //connection with client (only initialized when verified habbo connection)
    private volatile PacketHandler outHandler = null;    //connection with server (only initialized when verified habbo connection)

    private volatile String hotelVersion = "";
    private volatile String clientIdentifier = "";
    private volatile PacketInfoManager packetInfoManager = null;

    public HProxy(HClient hClient, String input_domain, String actual_domain, int actual_port, int intercept_port, String intercept_host) {
        this.hClient = hClient;
        this.input_domain = input_domain;
        this.actual_domain = actual_domain;
        this.actual_port = actual_port;
        this.intercept_host = intercept_host;
        this.intercept_port = intercept_port;
    }

    public void initProxy(ServerSocket socket) {
        this.proxy_server = socket;
    }

    public void verifyProxy(PacketHandler incomingHandler, PacketHandler outgoingHandler, String hotelVersion, String clientIdentifier) {
        this.inHandler = incomingHandler;
        this.outHandler = outgoingHandler;
        this.hotelVersion = hotelVersion;
        this.clientIdentifier = clientIdentifier;
        this.packetInfoManager = PacketInfoManager.fromHotelVersion(hotelVersion, hClient);

        SafePacketsContainer packetsContainer = PacketSafetyManager.PACKET_SAFETY_MANAGER.getPacketContainer(hotelVersion);
        for (PacketInfo packetInfo : packetInfoManager.getPacketInfoList()) {
            packetsContainer.validateSafePacket(packetInfo.getHeaderId(), packetInfo.getDestination());
        }
    }

    public boolean sendToServer(HPacket packet) {
        if (outHandler != null) {
            return outHandler.sendToStream(packet.toBytes());
        }
        return false;
    }

    public boolean sendToClient(HPacket packet) {
        if (inHandler != null) {
            return inHandler.sendToStream(packet.toBytes());
        }
        return false;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public int getActual_port() {
        return actual_port;
    }

    public int getIntercept_port() {
        return intercept_port;
    }

    public ServerSocket getProxy_server() {
        return proxy_server;
    }

    public String getActual_domain() {
        return actual_domain;
    }

    public String getInput_domain() {
        return input_domain;
    }

    public String getIntercept_host() {
        return intercept_host;
    }

    public PacketHandler getInHandler() {
        return inHandler;
    }

    public PacketHandler getOutHandler() {
        return outHandler;
    }

    public String getHotelVersion() {
        return hotelVersion;
    }

    public HClient getHClient() {
        return hClient;
    }

    public PacketInfoManager getPacketInfoManager() {
        return packetInfoManager;
    }

    @Override
    public String toString() {
        return "HProxy{" +
                "hClient=" + hClient +
                ", input_domain='" + input_domain + '\'' +
                ", actual_domain='" + actual_domain + '\'' +
                ", actual_port=" + actual_port +
                ", intercept_port=" + intercept_port +
                ", intercept_host='" + intercept_host + '\'' +
                ", proxy_server=" + proxy_server +
                ", inHandler=" + inHandler +
                ", outHandler=" + outHandler +
                ", hotelVersion='" + hotelVersion + '\'' +
                ", clientIdentifier='" + clientIdentifier + '\'' +
                '}';
    }
}
