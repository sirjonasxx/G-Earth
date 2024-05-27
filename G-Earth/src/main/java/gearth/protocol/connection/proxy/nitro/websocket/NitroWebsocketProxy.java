package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.nitro.NitroProxyProvider;
import gearth.protocol.connection.proxy.nitro.http.NitroCertificateSniffingManager;
import gearth.protocol.connection.proxy.nitro.http.NitroSslContextFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

public class NitroWebsocketProxy {

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection connection;
    private final NitroProxyProvider proxyProvider;
    private final NitroCertificateSniffingManager certificateManager;

    private final Server server;
    private final int serverPort;
    
    public NitroWebsocketProxy(HProxySetter proxySetter,
                               HStateSetter stateSetter,
                               HConnection connection,
                               NitroProxyProvider proxyProvider,
                               NitroCertificateSniffingManager certificateManager) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.connection = connection;
        this.proxyProvider = proxyProvider;
        this.certificateManager = certificateManager;
        this.server = new Server();
        this.serverPort = 0;
    }

    public boolean start() {
        try {
            // Configure SSL.
            final NitroSslContextFactory sslContextFactory = new NitroSslContextFactory(this.certificateManager);
            final ServerConnector sslConnector = new ServerConnector(server, sslContextFactory);

            sslConnector.setPort(this.serverPort);

            // Add SSL to the server.
            server.addConnector(sslConnector);

            // Configure the WebSocket.
            final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");

            final HandlerList handlers = new HandlerList();
            handlers.setHandlers(new Handler[] { context });

            final ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
            wscontainer.addEndpoint(ServerEndpointConfig.Builder
                    .create(NitroWebsocketClient.class, "/")
                    .configurator(new NitroWebsocketServerConfigurator(proxySetter, stateSetter, connection, proxyProvider))
                    .build());

            server.setHandler(handlers);
            server.start();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }

    public int getPort() {
        final ServerConnector serverConnector = (ServerConnector) server.getConnectors()[0];

        return serverConnector.getLocalPort();
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
