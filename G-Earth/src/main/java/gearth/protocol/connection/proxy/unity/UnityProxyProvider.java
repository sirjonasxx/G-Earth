package gearth.protocol.connection.proxy.unity;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.services.unity_tools.GUnityFileServer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;

import static gearth.services.unity_tools.GUnityFileServer.FILESERVER_PORT;

public class UnityProxyProvider implements ProxyProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(UnityProxyProvider.class);

    public static final int PORT_REQUESTER_SERVER_PORT = 9039;
    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection hConnection;

    private Server packetHandlerServer = null;

    public UnityProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.hConnection = hConnection;
    }

    @Override
    public void start() throws IOException {
        // https://happyhyppo.ro/2016/03/21/minimal-websockets-communication-with-javajetty-and-angularjs/

        try {
            int port = 9040;
            boolean fail = true;
            while (fail && port < 9100) {
                try {
                    packetHandlerServer = new Server(port);
                    final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                    context.setContextPath("/ws");

                    final HandlerList handlers = new HandlerList();
                    handlers.setHandlers(new Handler[] { context });

                    final ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
                    wscontainer.addEndpoint(ServerEndpointConfig.Builder
                            .create(UnityCommunicator.class, "/packethandler") // the endpoint url
                            .configurator(new UnityCommunicatorConfig(proxySetter, stateSetter, hConnection, this))
                            .build());
                    packetHandlerServer.setHandler(handlers);
                    packetHandlerServer.start();
                    fail = false;
                }
                catch (Exception e) {
                    port++;
                }
            }

            if (fail)
                throw new Exception();

            startPortRequestServer(port);
            startUnityFileServer();
            stateSetter.setState(HState.WAITING_FOR_CLIENT);

        } catch (Exception e) {
            stateSetter.setState(HState.NOT_CONNECTED);
            try {
                packetHandlerServer.stop();
            } catch (Exception ex) {
                LOGGER.error("Failed to close packet handler server", ex);
            }
            LOGGER.error("Failed to connect to unity proxy", e);
        }
    }

    @Override
    public synchronized void abort() {
        if (packetHandlerServer == null)
            return;

        final Server abortThis = packetHandlerServer;
        stateSetter.setState(HState.ABORTING);
        new Thread(() -> {
            try {
                abortThis.stop();
            } catch (Exception e) {
                LOGGER.error("Failed to abort", e);
            } finally {
                stateSetter.setState(HState.NOT_CONNECTED);
            }
        }).start();
        packetHandlerServer = null;
    }

    private void startUnityFileServer() throws Exception {
        final Server server = new Server(FILESERVER_PORT);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new GUnityFileServer()), "/*");

        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { context });
        server.setHandler(handlers);

        server.start();

        stopServerOnDisconnect(server);
    }

    private void startPortRequestServer(int packetHandlerPort) throws Exception {
        final Server portRequestServer = new Server(PORT_REQUESTER_SERVER_PORT);
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/ws");

        final HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { context });
        portRequestServer.setHandler(handlers);

        final ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
        wscontainer.addEndpoint(ServerEndpointConfig.Builder
                .create(PortRequester.class, "/portrequest") // the endpoint url
                .configurator(new PortRequesterConfig(packetHandlerPort))
                .build());

        portRequestServer.start();

        stopServerOnDisconnect(portRequestServer);
    }

    private void stopServerOnDisconnect(Server server) {
        hConnection.stateProperty().addListener(new ChangeListener<HState>() {
            @Override
            public void changed(ObservableValue<? extends HState> observable, HState oldValue, HState newValue) {
                if (oldValue == HState.WAITING_FOR_CLIENT || newValue == HState.NOT_CONNECTED) {
                    hConnection.stateProperty().removeListener(this);
                    try {
                        server.stop();
                    } catch (Exception e) {
                        LOGGER.error("Failed to stop server {}", server, e);
                    }
                }
            }
        });
    }
}
