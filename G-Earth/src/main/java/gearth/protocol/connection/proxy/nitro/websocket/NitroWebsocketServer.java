package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.packethandler.PacketHandler;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NitroWebsocketServer extends Endpoint implements NitroSession {

    private final PacketHandler packetHandler;
    private final NitroWebsocketClient client;
    private Session activeSession = null;

    public NitroWebsocketServer(HConnection connection, NitroWebsocketClient client) {
        this.client = client;
        this.packetHandler = new NitroPacketHandler(HMessage.Direction.TOCLIENT, client, connection.getExtensionHandler(), connection.getTrafficObservables());
    }

    public void connect(String websocketUrl, String originUrl) throws IOException {
        try {
            ClientEndpointConfig.Builder builder = ClientEndpointConfig.Builder.create();

            if (originUrl != null) {
                builder.configurator(new ClientEndpointConfig.Configurator() {
                    @Override
                    public void beforeRequest(Map<String, List<String>> headers) {
                        headers.put("Origin", Collections.singletonList(originUrl));
                    }
                });
            }

            ClientEndpointConfig config = builder.build();

            ContainerProvider.getWebSocketContainer().connectToServer(this, config, URI.create(websocketUrl));
        } catch (DeploymentException e) {
            throw new IOException("Failed to deploy websocket client", e);
        }
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        this.activeSession = session;
        this.activeSession.setMaxBinaryMessageBufferSize(NitroConstants.WEBSOCKET_BUFFER_SIZE);
        this.activeSession.addMessageHandler(new MessageHandler.Whole<byte[]>() {
            @Override
            public void onMessage(byte[] message) {
                try {
                    packetHandler.act(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        // Hotel closed connection.
        client.shutdownProxy();
    }

    @Override
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
