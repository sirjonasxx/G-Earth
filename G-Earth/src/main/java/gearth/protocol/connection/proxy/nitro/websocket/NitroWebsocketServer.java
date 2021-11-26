package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.packethandler.PacketHandler;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class NitroWebsocketServer implements NitroSession {

    private final PacketHandler packetHandler;
    private final NitroWebsocketClient client;
    private Session activeSession = null;

    public NitroWebsocketServer(HConnection connection, NitroWebsocketClient client) {
        this.client = client;
        this.packetHandler = new NitroPacketHandler(HMessage.Direction.TOCLIENT, client, connection.getExtensionHandler(), connection.getTrafficObservables());
    }

    public void connect(String websocketUrl) throws IOException {
        try {
            ContainerProvider.getWebSocketContainer().connectToServer(this, URI.create(websocketUrl));
        } catch (DeploymentException e) {
            throw new IOException("Failed to deploy websocket client", e);
        }
    }

    @OnOpen
    public void onOpen(Session Session) {
        this.activeSession = Session;
        this.activeSession.setMaxBinaryMessageBufferSize(NitroConstants.WEBSOCKET_BUFFER_SIZE);
    }

    @OnMessage
    public void onMessage(byte[] b, Session session) throws IOException {
        packetHandler.act(b);
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        // Hotel closed connection.
        client.shutdownProxy();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();

        // Shutdown.
        client.shutdownProxy();
    }

    @Override
    public Session getSession() {
        return activeSession;
    }

    public PacketHandler getPacketHandler() {
        return packetHandler;
    }

    /**
     * Shutdown and clean up the server connection.
     */
    public void shutdown() {
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
}
