package gearth.protocol.connection.proxy.unity;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;

public class UnityProxyProvider implements ProxyProvider {

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection hConnection;

    private Server server;

    public UnityProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.hConnection = hConnection;
    }


    @Override
    public void start() throws IOException {

        // https://happyhyppo.ro/2016/03/21/minimal-websockets-communication-with-javajetty-and-angularjs/
        server = new Server(8025);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/ws");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { context });
        server.setHandler(handlers);

        ServerContainer wscontainer = null;
        try {
            wscontainer = WebSocketServerContainerInitializer.configureContext(context);

            wscontainer.addEndpoint(ServerEndpointConfig.Builder
                    .create(UnityCommunicator.class, "/packethandler") // the endpoint url
                    .configurator(new UnityCommunicatorConfig(proxySetter, stateSetter, hConnection, this))
                    .build());

            server.start();
            stateSetter.setState(HState.WAITING_FOR_CLIENT);
        } catch (Exception e) {
            stateSetter.setState(HState.NOT_CONNECTED);
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void abort() {
        if (server == null) {
            return;
        }

        final Server abortThis = server;
        stateSetter.setState(HState.ABORTING);
        new Thread(() -> {
            try {
                abortThis.stop();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stateSetter.setState(HState.NOT_CONNECTED);
            }
        }).start();
        server = null;
    }
}
