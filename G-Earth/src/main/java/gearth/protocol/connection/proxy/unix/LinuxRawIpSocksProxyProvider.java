package gearth.protocol.connection.proxy.unix;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HStateSetter;

public class LinuxRawIpSocksProxyProvider extends LinuxRawIpProxyProvider {
    public LinuxRawIpSocksProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, String input_host, int input_port) {
        super(proxySetter, stateSetter, hConnection, input_host, input_port);
        useSocks = true;
    }
}
