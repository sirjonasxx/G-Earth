package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.StateChangeListener;
import gearth.protocol.connection.*;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.NitroPacketQueue;
import gearth.protocol.packethandler.nitro.NitroPacketHandler;
import gearth.services.nitro.NitroHotel;
import gearth.services.nitro.NitroHotelManager;
import gearth.services.nitro.NitroPacketModifier;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class NitroWebsocketHandler implements NitroWebsocketCallback, StateChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(NitroWebsocketHandler.class);

    private final NitroHotelManager nitroHotelManager;
    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection connection;
    private final NitroNettySessionProvider clientSessionProvider;
    private final NitroNettySessionProvider serverSessionProvider;
    private final NitroPacketHandler clientPacketHandler;
    private final NitroPacketHandler serverPacketHandler;
    private final NitroPacketQueue packetQueue;
    private final AtomicBoolean shutdownLock;
    private final AtomicBoolean isAborting;

    private NitroHotel nitroHotel;
    private NitroPacketModifier packetModifier;
    private boolean isHandshakeComplete;

    public NitroWebsocketHandler(NitroHotelManager nitroHotelManager, HProxySetter proxySetter, HStateSetter stateSetter, HConnection connection) {
        this.nitroHotelManager = nitroHotelManager;
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.connection = connection;
        this.clientSessionProvider = new NitroNettySessionProvider();
        this.clientPacketHandler = new NitroPacketHandler(HMessage.Direction.TOCLIENT, this.clientSessionProvider, connection.getExtensionHandler(), connection.getTrafficObservables());
        this.serverSessionProvider = new NitroNettySessionProvider();
        this.serverPacketHandler = new NitroPacketHandler(HMessage.Direction.TOSERVER, this.serverSessionProvider, connection.getExtensionHandler(), connection.getTrafficObservables());
        this.packetQueue = new NitroPacketQueue(this.serverPacketHandler);
        this.shutdownLock = new AtomicBoolean();
        this.isAborting = new AtomicBoolean(false);
    }

    @Override
    public void onConnected(String websocketUrl, Channel client, Channel server) {
        logger.info("Nitro websocket connected");

        // Setup sessions.
        final NitroNettySession clientSession = new NitroNettySession(client);
        final NitroNettySession serverSession = new NitroNettySession(server);

        // Setup nitro hotel.
        if (this.nitroHotelManager.hasWebsocket(websocketUrl)) {
            this.nitroHotel = this.nitroHotelManager.getByWebsocket(websocketUrl);

            final NitroPacketModifier modifier = this.nitroHotel.createPacketModifier();

            if (modifier != null) {
                this.packetModifier = modifier;

                clientSession.setModifier(data -> this.packetModifier.gearthToClient(data));
                serverSession.setModifier(data -> this.packetModifier.gearthToServer(data));
            }

            logger.info("Detected hotel as {}, using a custom nitro configuration", this.nitroHotel.getName());
        } else {
            logger.info("Using default nitro configuration");
        }
        this.clientSessionProvider.setSession(clientSession);
        this.serverSessionProvider.setSession(serverSession);

        // Setup proxy.
        final HProxy proxy = new HProxy(HClient.NITRO, "", "", -1, -1, "");

        proxy.verifyProxy(
                this.clientPacketHandler,
                this.serverPacketHandler,
                NitroConstants.WEBSOCKET_REVISION,
                NitroConstants.WEBSOCKET_CLIENT_IDENTIFIER
        );

        proxySetter.setProxy(proxy);

        // Register state listener.
        this.connection.getStateObservable().addListener(this);

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
        if (this.packetModifier != null) {
            try {
                buffer = this.packetModifier.clientToGearth(buffer);
            } catch (Exception e) {
                logger.error("Failed to modify clientToGearth packet", e);
                shutdownProxy();
                return;
            }
        }

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
        if (this.packetModifier != null) {
            try {
                buffer = this.packetModifier.serverToGearth(buffer);
            } catch (Exception e) {
                logger.error("Failed to modify serverToGearth packet", e);
                shutdownProxy();
                return;
            }
        }

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

            // Check if we are already aborting.
            if (!this.isAborting.get()) {
                this.stateSetter.setState(HState.ABORTING);
            }
        }
    }

    @Override
    public void stateChanged(HState oldState, HState newState) {
        if (newState == HState.ABORTING || newState == HState.NOT_CONNECTED) {
            this.isAborting.set(true);
            this.connection.getStateObservable().removeListener(this);
        }
    }
}
