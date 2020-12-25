package gearth.protocol.connection.proxy.unity;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;

import javax.websocket.server.ServerEndpointConfig;

public class UnityCommunicatorConfig extends ServerEndpointConfig.Configurator {
    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection hConnection;
    private final ProxyProvider proxyProvider;

    public UnityCommunicatorConfig(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, ProxyProvider proxyProvider) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.hConnection = hConnection;
        this.proxyProvider = proxyProvider;
    }


    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        return (T)new UnityCommunicator(proxySetter, stateSetter, hConnection, proxyProvider);
    }
}