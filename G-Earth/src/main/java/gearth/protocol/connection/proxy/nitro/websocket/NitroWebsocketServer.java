package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.nitro.NitroPacketHandler;
import org.eclipse.jetty.websocket.api.extensions.ExtensionConfig;
import org.eclipse.jetty.websocket.jsr356.JsrExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class NitroWebsocketServer extends Endpoint implements NitroSession {

    private static final Logger logger = LoggerFactory.getLogger(NitroWebsocketServer.class);

    private static final HashSet<String> SKIP_HEADERS = new HashSet<>(Arrays.asList(
            "Sec-WebSocket-Extensions",
            "Sec-WebSocket-Key",
            "Sec-WebSocket-Version",
            "Host",
            "Connection",
            "Upgrade"
    ));

    private final PacketHandler packetHandler;
    private final NitroWebsocketClient client;
    private Session activeSession = null;

    public NitroWebsocketServer(HConnection connection, NitroWebsocketClient client) {
        this.client = client;
        this.packetHandler = new NitroPacketHandler(HMessage.Direction.TOCLIENT, client, connection.getExtensionHandler(), connection.getTrafficObservables());
    }

    public void connect(String websocketUrl, Map<String, List<String>> clientHeaders) throws IOException {
        try {
            logger.info("Connecting to origin websocket at {}", websocketUrl);

            ClientEndpointConfig.Builder builder = ClientEndpointConfig.Builder.create();

            builder.extensions(Collections.singletonList(new JsrExtension(new ExtensionConfig("permessage-deflate;client_max_window_bits"))));

            builder.configurator(new ClientEndpointConfig.Configurator() {
                @Override
                public void beforeRequest(Map<String, List<String>> headers) {
                    clientHeaders.forEach((key, value) -> {
                        if (SKIP_HEADERS.contains(key)) {
                            return;
                        }

                        headers.remove(key);
                        headers.put(key, value);
                    });
                }
            });

            ClientEndpointConfig config = builder.build();

            ContainerProvider.getWebSocketContainer().connectToServer(this, config, URI.create(websocketUrl));

            logger.info("Connected to origin websocket");
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
