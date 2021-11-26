package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;

public class NitroForwarder {

    private final HConnection connection;
    private final NitroSession client;
    private final NitroSession server;

    public NitroForwarder(HConnection connection, NitroSession client, NitroSession server) {
        this.connection = connection;
        this.client = client;
        this.server = server;
    }

}
