package gearth.app.protocol.connection.proxy.unity;

import gearth.app.protocol.HConnection;
import gearth.app.protocol.connection.HProxySetter;
import gearth.app.protocol.connection.HStateSetter;
import gearth.app.protocol.connection.proxy.ProxyProvider;

public record UnityCommunicatorConfig(HProxySetter proxySetter,
                                      HStateSetter stateSetter,
                                      HConnection hConnection,
                                      ProxyProvider proxyProvider) {
}