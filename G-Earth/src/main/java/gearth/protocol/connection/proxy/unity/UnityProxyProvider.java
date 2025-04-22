package gearth.protocol.connection.proxy.unity;

import gearth.protocol.HConnection;
import gearth.protocol.StateChangeListener;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.connection.proxy.http.HttpProxyManager;
import gearth.services.unity_tools.GUnityFileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class UnityProxyProvider implements ProxyProvider, StateChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(UnityProxyProvider.class);

    private final HStateSetter stateSetter;
    private final HConnection hConnection;
    private final UnityWebsocketServer websocketServer;
    private final HttpProxyManager httpProxy;

    public UnityProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection) {
        this.stateSetter = stateSetter;
        this.hConnection = hConnection;
        this.websocketServer = new UnityWebsocketServer(new UnityCommunicatorConfig(proxySetter, stateSetter, hConnection, this));
        this.httpProxy = new HttpProxyManager();
    }

    @Override
    public void start() throws IOException {
        try {
            hConnection.getStateObservable().addListener(this);

            LOG.info("Starting Unity Websocket Server");

            if (!this.websocketServer.start()) {
                LOG.error("Failed to start unity websocket server");
                abort();
                return;
            }

            LOG.info("Unity websocket server started on port {}", this.websocketServer.getPort());

            LOG.info("Starting unity http proxy");

            if (!this.httpProxy.start(new GUnityFileServer(this.websocketServer.getPort()))) {
                LOG.error("Failed to start nitro proxy");
                abort();
                return;
            }

            LOG.info("Unity http proxy started");

            stateSetter.setState(HState.WAITING_FOR_CLIENT);
        } catch (Exception e) {
            LOG.error("Failed to start unity proxy", e);

            abort();
        }
    }

    @Override
    public synchronized void abort() {
        stateSetter.setState(HState.ABORTING);

        new Thread(() -> {
            LOG.info("Stopping unity websocket server");

            try {
                websocketServer.stop();
            } catch (Exception ex) {
                LOG.error("Failed to stop unity websocket server", ex);
            } finally {
                LOG.info("Unity websocket server stopped");
            }

            LOG.info("Stopping unity http proxy");

            try {
                httpProxy.stop();
            } catch (Exception e) {
                LOG.error("Failed to stop unity http proxy", e);
            } finally {
                LOG.info("Unity http proxy stopped");
            }

            stateSetter.setState(HState.NOT_CONNECTED);
        }).start();
    }

    @Override
    public void stateChanged(HState oldState, HState newState) {
        if (oldState == HState.WAITING_FOR_CLIENT && newState == HState.CONNECTED) {
            // Unregister but do not stop http proxy.
            // We are not stopping the http proxy itself because the hotel websocket is connected to it.
            httpProxy.pause();
            LOG.info("Unity proxy paused");
        }
    }
}
