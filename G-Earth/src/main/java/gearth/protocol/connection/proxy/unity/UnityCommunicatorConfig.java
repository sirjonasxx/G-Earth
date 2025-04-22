package gearth.protocol.connection.proxy.unity;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;

public record UnityCommunicatorConfig(HProxySetter proxySetter,
                                      HStateSetter stateSetter,
                                      HConnection hConnection,
                                      ProxyProvider proxyProvider) {
}