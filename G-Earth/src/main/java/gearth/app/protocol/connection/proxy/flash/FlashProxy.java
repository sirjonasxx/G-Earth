package gearth.app.protocol.connection.proxy.flash;

import gearth.app.protocol.HConnection;
import gearth.app.protocol.connection.HProxy;
import gearth.app.protocol.connection.HProxySetter;
import gearth.app.protocol.connection.HState;
import gearth.app.protocol.connection.HStateSetter;
import gearth.protocol.connection.*;
import gearth.app.protocol.interceptor.ConnectionInterceptor;
import gearth.app.protocol.interceptor.ConnectionInterceptorCallbacks;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class FlashProxy extends FlashProxyProvider implements ConnectionInterceptorCallbacks {

    private final ConnectionInterceptor interceptor;

    public FlashProxy(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, List<String> potentialHosts, boolean useSocks) {
        super(proxySetter, stateSetter, hConnection);
        this.interceptor = new ConnectionInterceptor(HClient.FLASH, stateSetter, hConnection, this, potentialHosts, useSocks);
    }

    @Override
    public void start() throws IOException {
        if (hConnection.getState() != HState.NOT_CONNECTED) {
            return;
        }

        interceptor.start();
    }

    @Override
    public void abort() {
        stateSetter.setState(HState.ABORTING);
        interceptor.stop(false);
        super.abort();
    }

    @Override
    protected void onConnect() {
        super.onConnect();
        interceptor.stop(true);
    }

    @Override
    public void onInterceptorConnected(Socket client, Socket server, HProxy proxy) throws IOException, InterruptedException {
        startProxyThread(client, server, proxy);
    }

    @Override
    public void onInterceptorError() {
        showInvalidConnectionError();
        abort();
    }
}
