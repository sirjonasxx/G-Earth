package gearth.protocol.connection.proxy.nitro;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.connection.proxy.nitro.http.NitroHttpProxy;

import java.io.IOException;

public class NitroProxyProvider implements ProxyProvider {

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection hConnection;
    private final NitroHttpProxy nitroProxy;

    public NitroProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.hConnection = hConnection;
        this.nitroProxy = new NitroHttpProxy();
    }

    @Override
    public void start() throws IOException {
        if (!nitroProxy.start()) {
            System.out.println("Failed to start nitro proxy");

            stateSetter.setState(HState.NOT_CONNECTED);
            return;
        }

        stateSetter.setState(HState.WAITING_FOR_CLIENT);
    }

    @Override
    public void abort() {
        stateSetter.setState(HState.ABORTING);

        new Thread(() -> {
            try {
                nitroProxy.stop();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stateSetter.setState(HState.NOT_CONNECTED);
            }
        }).start();
    }

}
