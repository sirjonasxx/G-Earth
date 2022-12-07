package gearth.protocol.connection.proxy.nitro;

import gearth.protocol.HConnection;
import gearth.protocol.StateChangeListener;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.connection.proxy.nitro.http.NitroHttpProxy;
import gearth.protocol.connection.proxy.nitro.http.NitroHttpProxyServerCallback;
import gearth.protocol.connection.proxy.nitro.websocket.NitroWebsocketProxy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NitroProxyProvider implements ProxyProvider, NitroHttpProxyServerCallback, StateChangeListener {

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection connection;
    private final NitroHttpProxy nitroHttpProxy;
    private final NitroWebsocketProxy nitroWebsocketProxy;
    private final AtomicBoolean abortLock;

    private int websocketPort;
    private String originalWebsocketUrl;
    private String originalCookies;

    public NitroProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection connection) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.connection = connection;
        this.nitroHttpProxy = new NitroHttpProxy(this);
        this.nitroWebsocketProxy = new NitroWebsocketProxy(proxySetter, stateSetter, connection, this);
        this.abortLock = new AtomicBoolean();
    }

    public String getOriginalWebsocketUrl() {
        return originalWebsocketUrl;
    }

    public String getOriginalCookies() {
        return originalCookies;
    }

    @Override
    public void start() throws IOException {
        originalWebsocketUrl = null;
        originalCookies = null;

        connection.getStateObservable().addListener(this);

        if (!nitroHttpProxy.start()) {
            System.out.println("Failed to start nitro proxy");
            abort();
            return;
        }

        if (!nitroWebsocketProxy.start()) {
            System.out.println("Failed to start nitro websocket proxy");
            abort();
            return;
        }

        websocketPort = nitroWebsocketProxy.getPort();

        stateSetter.setState(HState.WAITING_FOR_CLIENT);
    }

    @Override
    public void abort() {
        if (abortLock.get()) {
            return;
        }

        if (abortLock.compareAndSet(true, true)) {
            return;
        }

        stateSetter.setState(HState.ABORTING);

        new Thread(() -> {
            try {
                nitroHttpProxy.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                nitroWebsocketProxy.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            stateSetter.setState(HState.NOT_CONNECTED);

            connection.getStateObservable().removeListener(this);
        }).start();
    }

    @Override
    public String replaceWebsocketServer(String configUrl, String websocketUrl) {
        originalWebsocketUrl = websocketUrl;

        return String.format("ws://127.0.0.1:%d", websocketPort);
    }

    @Override
    public void setOriginCookies(String cookieHeaderValue) {
        originalCookies = cookieHeaderValue;
    }

    @Override
    public void stateChanged(HState oldState, HState newState) {
        if (oldState == HState.WAITING_FOR_CLIENT && newState == HState.CONNECTED) {
            // Unregister but do not stop http proxy.
            // We are not stopping the http proxy because some requests might still require it to be running.
            nitroHttpProxy.pause();
        }

        // Catch setState ABORTING inside NitroWebsocketClient.
        if (newState == HState.ABORTING) {
            abort();
        }
    }
}
