package gearth.protocol.connection.proxy;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class SocksProxyProvider extends RawIpProxyProvider {

    private static SocksConfiguration socksConfig = null;

    public SocksProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, String input_host, int input_port) {
        super(proxySetter, stateSetter, hConnection, input_host, input_port);
    }

    @Override
    protected void onBeforeIpMapping() {
        // do nothing
    }

    @Override
    protected void createProxyThread(Socket client) throws IOException, InterruptedException {
        if (socksConfig == null) {
            maybeRemoveMapping();
            stateSetter.setState(HState.NOT_CONNECTED);
            showInvalidConnectionError();
            return;
        }

        Proxy socks = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(socksConfig.getSocksHost(), socksConfig.getSocksPort()));
        Socket server = new Socket(socks);
        server.setSoTimeout(1200);
        try {
            server.connect(new InetSocketAddress(proxy.getActual_domain(), proxy.getActual_port()), 1200);
            startProxyThread(client, server, proxy);
        }
        catch (SocketTimeoutException e) {
            maybeRemoveMapping();
            stateSetter.setState(HState.NOT_CONNECTED);
            showInvalidConnectionError();
        }
    }

    public static void setSocksConfig(SocksConfiguration configuration) {
        socksConfig = configuration;
    }

    public static SocksConfiguration getSocksConfig() {
        return socksConfig;
    }
}
