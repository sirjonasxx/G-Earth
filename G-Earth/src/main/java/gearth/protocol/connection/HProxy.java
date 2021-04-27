package gearth.protocol.connection;

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

    private volatile PacketSenderQueue packetSenderQueue = null;

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
        this.packetSenderQueue = new PacketSenderQueue(this);
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

    public PacketSenderQueue getPacketSenderQueue() {
        return packetSenderQueue;
    }

    public HClient gethClient() {
        return hClient;
    }

    public PacketInfoManager getPacketInfoManager() {
        return packetInfoManager;
    }
}
