package gearth.protocol.connection.proxy.windows;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.SocksConfiguration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class WindowsRawIpSocksProxyProvider extends WindowsRawIpProxyProvider {

    public WindowsRawIpSocksProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, String input_host, int input_port) {
        super(proxySetter, stateSetter, hConnection, input_host, input_port);
        useSocks = true;
    }
}
