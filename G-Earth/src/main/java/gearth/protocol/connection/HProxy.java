package gearth.protocol.connection;

import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.flash.IncomingFlashPacketHandler;
import gearth.protocol.packethandler.flash.OutgoingFlashPacketHandler;

import java.net.ServerSocket;

public class HProxy {
    private volatile String input_domain;           //string representation of the domain to intercept
    private volatile String actual_domain;          //dns resolved domain (ignoring hosts file)
    private volatile int actual_port;               //port of the server

    private volatile int intercept_port;            //port used to intercept connection (with the current implementation, must equal actual_port)
    private volatile String intercept_host;         //local ip used to intercept host, example 127.0.0.1

    private volatile ServerSocket proxy_server = null;     //listener for the client

    private volatile PacketHandler inHandler = null;     //connection with client (only initialized when verified habbo connection)
    private volatile PacketHandler outHandler = null;    //connection with server (only initialized when verified habbo connection)

    private volatile String hotelVersion = "";
    private volatile AsyncPacketSender asyncPacketSender = null;

    public HProxy(String input_domain, String actual_domain, int actual_port, int intercept_port, String intercept_host) {
        this.input_domain = input_domain;
        this.actual_domain = actual_domain;
        this.actual_port = actual_port;
        this.intercept_host = intercept_host;
        this.intercept_port = intercept_port;
    }

    public void initProxy(ServerSocket socket) {
        this.proxy_server = socket;
    }

    public void verifyProxy(PacketHandler incomingHandler, PacketHandler outgoingHandler, String hotelVersion) {
        this.inHandler = incomingHandler;
        this.outHandler = outgoingHandler;
        this.hotelVersion = hotelVersion;
        this.asyncPacketSender = new AsyncPacketSender(this);
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

    public AsyncPacketSender getAsyncPacketSender() {
        return asyncPacketSender;
    }
}
