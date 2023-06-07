package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.StateChangeListener;
import gearth.protocol.connection.*;
import gearth.protocol.connection.proxy.nitro.NitroConnectionState;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.NitroPacketQueue;
import gearth.protocol.connection.proxy.nitro.NitroProxyProvider;
import gearth.protocol.packethandler.nitro.NitroPacketHandler;
import org.eclipse.jetty.websocket.jsr356.JsrSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@ServerEndpoint(value = "/")
public class NitroWebsocketClient implements NitroSession {

    private static final Logger logger = LoggerFactory.getLogger(NitroWebsocketClient.class);

    private final HProxySetter proxySetter;
    private final HConnection connection;
    private final NitroConnectionState state;
    private final NitroProxyProvider proxyProvider;
    private final NitroWebsocketServer server;
    private final NitroPacketHandler packetHandler;
    private final NitroPacketQueue packetQueue;
    private final AtomicBoolean shutdownLock;

    private JsrSession activeSession = null;

    public NitroWebsocketClient(HProxySetter proxySetter, HStateSetter stateSetter, HConnection connection, NitroProxyProvider proxyProvider) {
        this.proxySetter = proxySetter;
        this.connection = connection;
        this.state = new NitroConnectionState(stateSetter);
        this.proxyProvider = proxyProvider;
        this.server = new NitroWebsocketServer(connection, this, this.state);
        this.packetHandler = new NitroPacketHandler(HMessage.Direction.TOSERVER, server, connection.getExtensionHandler(), connection.getTrafficObservables());
        this.packetQueue = new NitroPacketQueue(this.packetHandler);
        this.shutdownLock = new AtomicBoolean();
    }

    @OnOpen
    public void onOpen(Session session) throws Exception {
        logger.info("WebSocket connection accepted");

        // Setup state change listener
        connection.getStateObservable().addListener(new StateChangeListener() {
            @Override
            public void stateChanged(HState oldState, HState newState) {
                // Clean up when we don't need it anymore.
                if ((oldState == HState.WAITING_FOR_CLIENT || newState == HState.NOT_CONNECTED) || newState == HState.ABORTING) {
                    connection.getStateObservable().removeListener(this);
                }

                // Process queue when connected.
                try {
                    packetQueue.flush();
                } catch (IOException e) {
                    logger.error("Failed to flush packet queue in state change listener", e);
                }
            }
        });

        activeSession = (JsrSession) session;
        activeSession.setMaxBinaryMessageBufferSize(NitroConstants.WEBSOCKET_BUFFER_SIZE);
        activeSession.setMaxTextMessageBufferSize(NitroConstants.WEBSOCKET_BUFFER_SIZE);

        // Set proper headers to spoof being a real client.
        final Map<String, List<String>> headers = new HashMap<>(activeSession.getUpgradeRequest().getHeaders());

        if (proxyProvider.getOriginalCookies() != null) {
            headers.put("Cookie", Collections.singletonList(proxyProvider.getOriginalCookies()));
        }

        // Connect to origin server.
        server.connect(proxyProvider.getOriginalWebsocketUrl(), headers);

        final HProxy proxy = new HProxy(HClient.NITRO, "", "", -1, -1, "");

        proxy.verifyProxy(
                this.server.getPacketHandler(),
                this.packetHandler,
                NitroConstants.WEBSOCKET_REVISION,
                NitroConstants.WEBSOCKET_CLIENT_IDENTIFIER
        );

        proxySetter.setProxy(proxy);
        state.setConnected(HMessage.Direction.TOSERVER);
    }

    @OnMessage
    public void onMessage(byte[] b, Session session) throws IOException {
        logger.debug("Received packet from browser");

        // Enqueue all packets we receive to ensure we preserve correct packet order.
        packetQueue.enqueue(b);

        // Flush everything if we are connected.
        // We also flush when connection state changes to connected.
        if (state.isConnected()) {
            packetQueue.flush();
        }
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        activeSession = null;
        shutdownProxy();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();

        // Shutdown.
        shutdownProxy();
    }

    @Override
    public org.eclipse.jetty.websocket.api.Session getSession() {
        return activeSession;
    }

    /**
     * Shutdown and clean up the client connection.
     */
    private void shutdown() {
        if (activeSession == null) {
            return;
        }

        try {
            activeSession.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            activeSession = null;
        }
    }

    /**
     * Shutdown all connections and reset program state.
     */
    public void shutdownProxy() {
        if (shutdownLock.get()) {
            return;
        }

        if (shutdownLock.compareAndSet(false, true)) {
            // Close client connection.
            shutdown();

            // Close server connection.
            server.shutdown();

            // Reset program state.
            proxySetter.setProxy(null);
            state.setAborting();
        }
    }
}
