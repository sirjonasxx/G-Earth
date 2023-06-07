package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.nitro.NitroPacketHandler;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class NitroWebsocketServer implements WebSocketListener, NitroSession {

    private static final Logger logger = LoggerFactory.getLogger(NitroWebsocketServer.class);

    private static final HashSet<String> SKIP_HEADERS = new HashSet<>(Arrays.asList(
            "Sec-WebSocket-Extensions",
            "Sec-WebSocket-Key",
            "Sec-WebSocket-Version",
            "Host",
            "Connection",
            "Upgrade",
            "User-Agent", // Added by default
            "Accept-Encoding", // Added by default
            "Cache-Control", // Added by default
            "Pragma" // Added by default
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
            logger.info("Building origin websocket connection ({})", websocketUrl);

            final WebSocketClient client = createWebSocketClient();

            final ClientUpgradeRequest request = new ClientUpgradeRequest();

            request.addExtensions("permessage-deflate");

            clientHeaders.forEach((key, value) -> {
                if (SKIP_HEADERS.contains(key)) {
                    return;
                }

                request.setHeader(key, value);
            });

            if (clientHeaders.containsKey("User-Agent")) {
                final String realUserAgent = clientHeaders.get(HttpHeader.USER_AGENT.toString()).get(0);
                final HttpField clientUserAgent = new HttpField(HttpHeader.USER_AGENT, realUserAgent);

                client.getHttpClient().setUserAgentField(clientUserAgent);
            }

            logger.info("Connecting to origin websocket at {}", websocketUrl);

            client.start();
            client.connect(this, URI.create(websocketUrl), request);

            logger.info("Connected to origin websocket");
        } catch (Exception e) {
            throw new IOException("Failed to start websocket client to origin " + websocketUrl, e);
        }
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            activeSession = null;
        }
    }

    @Override
    public void onWebSocketBinary(byte[] bytes, int i, int i1) {
        try {
            packetHandler.act(bytes);
        } catch (IOException e) {
            logger.error("Failed to handle packet", e);
        }
    }

    @Override
    public void onWebSocketText(String s) {
        logger.warn("Received text message from hotel");
    }

    @Override
    public void onWebSocketClose(int i, String s) {
        // Hotel closed connection.
        client.shutdownProxy();
    }

    @Override
    public void onWebSocketConnect(org.eclipse.jetty.websocket.api.Session session) {
        activeSession = session;
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        throwable.printStackTrace();

        // Shutdown.
        client.shutdownProxy();
    }

    private SSLContext createSSLContext() {
        final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(null, trustAllCerts, new SecureRandom());

            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup ssl context", e);
        }
    }

    private HttpClient createHttpClient() {
        final SslContextFactory.Client factory = new SslContextFactory.Client();

        factory.setSslContext(createSSLContext());

        return new HttpClient(factory);
    }

    private WebSocketClient createWebSocketClient() {
        final WebSocketClient client = new WebSocketClient(createHttpClient());

        client.setMaxBinaryMessageBufferSize(NitroConstants.WEBSOCKET_BUFFER_SIZE);
        client.setMaxTextMessageBufferSize(NitroConstants.WEBSOCKET_BUFFER_SIZE);

        return client;
    }
}
