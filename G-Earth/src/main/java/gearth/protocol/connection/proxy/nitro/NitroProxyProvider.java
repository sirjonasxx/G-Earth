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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

public class NitroProxyProvider implements ProxyProvider, NitroHttpProxyServerCallback, ChangeListener<HState> {

    private static final Logger logger = LoggerFactory.getLogger(NitroProxyProvider.class);

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

        connection.stateProperty().addListener(this);

        logger.info("Starting http proxy");

        if (!nitroHttpProxy.start()) {
            logger.error("Failed to start nitro proxy");
            abort();
            return;
        }

        logger.info("Starting websocket proxy");

        if (!nitroWebsocketProxy.start()) {
            logger.error("Failed to start nitro websocket proxy");
            abort();
            return;
        }

        websocketPort = nitroWebsocketProxy.getPort();

        logger.info("Websocket proxy is listening on port {}", websocketPort);
        logger.info("Nitro proxy started");

        stateSetter.setState(HState.WAITING_FOR_CLIENT);
    }

    @Override
    public void abort() {
        if (abortLock.get()) {
            return;
        }

        if (!abortLock.compareAndSet(false, true)) {
            return;
        }

        logger.info("Aborting nitro proxy");

        stateSetter.setState(HState.ABORTING);

        new Thread(() -> {
            logger.info("Stopping nitro http proxy");

            try {
                nitroHttpProxy.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            logger.info("Stopping nitro websocket proxy");

            try {
                nitroWebsocketProxy.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }

            stateSetter.setState(HState.NOT_CONNECTED);

            connection.stateProperty().removeListener(this);

            logger.info("Nitro proxy stopped");
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
    public void changed(ObservableValue<? extends HState> observable, HState oldValue, HState newValue) {
        if (oldValue == HState.WAITING_FOR_CLIENT && newValue == HState.CONNECTED) {
            // Unregister but do not stop http proxy.
            // We are not stopping the http proxy because some requests might still require it to be running.
            nitroHttpProxy.pause();
        }

        // Catch setState ABORTING inside NitroWebsocketClient.
        if (newValue == HState.ABORTING)
            abort();
    }
}
