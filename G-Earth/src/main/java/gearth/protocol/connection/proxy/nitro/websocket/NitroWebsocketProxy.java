package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.NitroProxyProvider;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
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

    private final Server server;
    
    public NitroWebsocketProxy(HProxySetter proxySetter, HStateSetter stateSetter, HConnection connection, NitroProxyProvider proxyProvider) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.connection = connection;
        this.proxyProvider = proxyProvider;
        this.server = new Server(NitroConstants.WEBSOCKET_PORT);
    }

    public boolean start() {
        try {
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

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
