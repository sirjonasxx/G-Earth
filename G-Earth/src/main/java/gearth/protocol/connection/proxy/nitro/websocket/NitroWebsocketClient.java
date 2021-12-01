package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.connection.*;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.NitroProxyProvider;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@ServerEndpoint(value = "/")
public class NitroWebsocketClient implements NitroSession {

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection connection;
    private final NitroProxyProvider proxyProvider;
    private final NitroWebsocketServer server;
    private final NitroPacketHandler packetHandler;
    private final AtomicBoolean shutdownLock;

    private Session activeSession = null;

    public NitroWebsocketClient(HProxySetter proxySetter, HStateSetter stateSetter, HConnection connection, NitroProxyProvider proxyProvider) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.connection = connection;
        this.proxyProvider = proxyProvider;
        this.server = new NitroWebsocketServer(connection, this);
        this.packetHandler = new NitroPacketHandler(HMessage.Direction.TOSERVER, server, connection.getExtensionHandler(), connection.getTrafficObservables());
        this.shutdownLock = new AtomicBoolean();
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        activeSession = session;
        activeSession.setMaxBinaryMessageBufferSize(NitroConstants.WEBSOCKET_BUFFER_SIZE);

        server.connect(proxyProvider.getOriginalWebsocketUrl(), proxyProvider.getOriginalOriginUrl());

        final HProxy proxy = new HProxy(HClient.NITRO, "", "", -1, -1, "");

        proxy.verifyProxy(
                this.server.getPacketHandler(),
                this.packetHandler,
                NitroConstants.WEBSOCKET_REVISION,
                NitroConstants.WEBSOCKET_CLIENT_IDENTIFIER
        );

        proxySetter.setProxy(proxy);
        stateSetter.setState(HState.CONNECTED);
    }

    @OnMessage
    public void onMessage(byte[] b, Session session) throws IOException {
        packetHandler.act(b);
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
    public Session getSession() {
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
        } catch (IOException e) {
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
            stateSetter.setState(HState.NOT_CONNECTED);
        }
    }
}
