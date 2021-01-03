package gearth.protocol.connection.proxy.unity;

import javax.websocket.server.ServerEndpointConfig;

public class PortRequesterConfig extends ServerEndpointConfig.Configurator {

    private final int packetHandlerPort;

    public PortRequesterConfig(int packetHandlerPort) {
        this.packetHandlerPort = packetHandlerPort;
    }

    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        return (T)new PortRequester(packetHandlerPort);
    }

}
