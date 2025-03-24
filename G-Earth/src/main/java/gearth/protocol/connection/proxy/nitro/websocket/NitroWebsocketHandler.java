package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.connection.*;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.NitroPacketQueue;
import gearth.protocol.connection.proxy.nitro.NitroProxyProvider;
import gearth.protocol.packethandler.nitro.NitroPacketHandler;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NitroWebsocketHandler implements NitroWebsocketCallback {

    private static final Logger logger = LoggerFactory.getLogger(NitroWebsocketHandler.class);

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final NitroNettySessionProvider clientSessionProvider;
    private final NitroNettySessionProvider serverSessionProvider;
    private final NitroPacketHandler clientPacketHandler;
    private final NitroPacketHandler serverPacketHandler;
    private final NitroPacketQueue packetQueue;
    private final AtomicBoolean shutdownLock;
    private boolean isHandshakeComplete;

    public NitroWebsocketHandler(HProxySetter proxySetter, HStateSetter stateSetter, HConnection connection, NitroProxyProvider proxyProvider) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.clientSessionProvider = new NitroNettySessionProvider();
        this.clientPacketHandler = new NitroPacketHandler(HMessage.Direction.TOCLIENT, this.clientSessionProvider, connection.getExtensionHandler(), connection.getTrafficObservables());
        this.serverSessionProvider = new NitroNettySessionProvider();
        this.serverPacketHandler = new NitroPacketHandler(HMessage.Direction.TOSERVER, this.serverSessionProvider, connection.getExtensionHandler(), connection.getTrafficObservables());
        this.packetQueue = new NitroPacketQueue(this.serverPacketHandler);
        this.shutdownLock = new AtomicBoolean();
    }

    @Override
    public void onConnected(Channel client, Channel server) {
        logger.info("Websocket connected");

        // Setup sessions.
        this.clientSessionProvider.setSession(new NitroNettySession(client));
        this.serverSessionProvider.setSession(new NitroNettySession(server));

        // Setup proxy.
        final HProxy proxy = new HProxy(HClient.NITRO, "", "", -1, -1, "");

        proxy.verifyProxy(
                this.clientPacketHandler,
                this.serverPacketHandler,
                NitroConstants.WEBSOCKET_REVISION,
                NitroConstants.WEBSOCKET_CLIENT_IDENTIFIER
        );

        proxySetter.setProxy(proxy);

        // Set state to connected.
        this.stateSetter.setState(HState.CONNECTED);
    }

    @Override
    public void onHandshakeComplete() {
        logger.info("Websocket handshake completed");

        // Mark handshake as complete.
        this.isHandshakeComplete = true;

        // Handle queued packets.
        try {
            packetQueue.flushAndAct();
        } catch (IOException e) {
            logger.error("Failed to flush packet queue after handshake", e);
        }
    }

    @Override
    public void onClose() {
        logger.info("Websocket disconnected");

        shutdownProxy();
    }

    @Override
    public void onClientMessage(byte[] buffer) {
        this.packetQueue.enqueue(buffer);

        if (this.isHandshakeComplete) {
            try {
                this.packetQueue.flushAndAct();
            } catch (IOException e) {
                logger.error("Failed to handle client packet", e);
            }
        }
    }

    @Override
    public void onServerMessage(byte[] buffer) {
        try {
            this.clientPacketHandler.act(buffer);
        } catch (IOException e) {
            logger.error("Failed to handle server packet", e);
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
            // Reset program state.
            this.proxySetter.setProxy(null);
            this.stateSetter.setState(HState.ABORTING);
        }
    }
}
