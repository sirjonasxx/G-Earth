package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.nitro.NitroProxyProvider;

import javax.websocket.server.ServerEndpointConfig;

public class NitroWebsocketServerConfigurator extends ServerEndpointConfig.Configurator {

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection connection;
    private final NitroProxyProvider proxyProvider;

    public NitroWebsocketServerConfigurator(HProxySetter proxySetter, HStateSetter stateSetter, HConnection connection, NitroProxyProvider proxyProvider) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.connection = connection;
        this.proxyProvider = proxyProvider;
    }

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) {
        return (T) new NitroWebsocketClient(proxySetter, stateSetter, connection, proxyProvider);
    }
}
