package gearth.protocol.connection.proxy.nitro;

import gearth.protocol.HConnection;
import gearth.protocol.StateChangeListener;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.connection.proxy.nitro.http.NitroHttpProxy;
import gearth.protocol.connection.proxy.nitro.websocket.NitroWebsocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NitroProxyProvider implements ProxyProvider, StateChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(NitroProxyProvider.class);

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection connection;
    private final NitroWebsocketHandler nitroWebsocketHandler;
    private final NitroHttpProxy nitroHttpProxy;
    private final AtomicBoolean abortLock;

    public NitroProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection connection) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.connection = connection;
        this.nitroWebsocketHandler = new NitroWebsocketHandler(proxySetter, stateSetter, connection, this);
        this.nitroHttpProxy = new NitroHttpProxy(this.nitroWebsocketHandler);
        this.abortLock = new AtomicBoolean();
    }

    @Override
    public void start() throws IOException {
        connection.getStateObservable().addListener(this);

        logger.info("Starting http proxy");

        if (!nitroHttpProxy.start()) {
            logger.error("Failed to start nitro proxy");
            abort();
            return;
        }

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
                logger.error("Failed to stop nitro http proxy", e);
            }

            stateSetter.setState(HState.NOT_CONNECTED);

            connection.getStateObservable().removeListener(this);

            logger.info("Nitro proxy stopped");
        }).start();
    }

    @Override
    public void stateChanged(HState oldState, HState newState) {
        if (oldState == HState.WAITING_FOR_CLIENT && newState == HState.CONNECTED) {
            // Unregister but do not stop http proxy.
            // We are not stopping the http proxy itself because the hotel websocket is connected to it.
            nitroHttpProxy.pause();
            logger.info("Nitro proxy paused");
        }

        // Catch setState ABORTING inside NitroWebsocketClient.
        if (newState == HState.ABORTING) {
            abort();
        }
    }
}
