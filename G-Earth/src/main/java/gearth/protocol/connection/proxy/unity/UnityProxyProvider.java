package gearth.protocol.connection.proxy.unity;

import gearth.protocol.HConnection;
import gearth.protocol.StateChangeListener;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.connection.proxy.http.HttpProxyManager;
import gearth.services.unity_tools.GUnityFileServer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;

public class UnityProxyProvider implements ProxyProvider, StateChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(UnityProxyProvider.class);

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection hConnection;
    private final HttpProxyManager httpProxy;

    private Server packetHandlerServer = null;

    public UnityProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.hConnection = hConnection;
        this.httpProxy = new HttpProxyManager();
    }


    @Override
    public void start() throws IOException {
        // https://happyhyppo.ro/2016/03/21/minimal-websockets-communication-with-javajetty-and-angularjs/

        try {
            LOG.info("Starting unity http proxy");

            hConnection.getStateObservable().addListener(this);

            if (!this.httpProxy.start(new GUnityFileServer())) {
                LOG.error("Failed to start nitro proxy");
                abort();
                return;
            }

            LOG.info("Unity http proxy started");

            int port = 9040;
            boolean fail = true;
            while (fail && port < 9100) {
                try {
                    packetHandlerServer = new Server(port);
                    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
                    context.setContextPath("/ws");

                    HandlerList handlers = new HandlerList();
                    handlers.setHandlers(new Handler[] { context });

                    ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
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

            if (fail) {
                throw new Exception();
            }


            startPortRequestServer(port);
            stateSetter.setState(HState.WAITING_FOR_CLIENT);

        } catch (Exception e) {
            stateSetter.setState(HState.NOT_CONNECTED);
            try {
                packetHandlerServer.stop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void abort() {
        if (packetHandlerServer == null) {
            return;
        }

        final Server abortThis = packetHandlerServer;
        stateSetter.setState(HState.ABORTING);
        new Thread(() -> {
            LOG.info("Stopping unity http proxy");

            try {
                httpProxy.stop();
            } catch (Exception e) {
                LOG.error("Failed to stop unity http proxy", e);
            }

            LOG.info("Unity http proxy stopped");

            try {
                abortThis.stop();
            } catch (Exception e) {
                LOG.error("Failed to stop unity packet handler server", e);
            } finally {
                stateSetter.setState(HState.NOT_CONNECTED);
            }
        }).start();
        packetHandlerServer = null;
    }

    private void startPortRequestServer(int packetHandlerPort) throws Exception {
        Server portRequestServer = new Server(9039);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/ws");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { context });
        portRequestServer.setHandler(handlers);

        ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
        wscontainer.addEndpoint(ServerEndpointConfig.Builder
                .create(PortRequester.class, "/portrequest") // the endpoint url
                .configurator(new PortRequesterConfig(packetHandlerPort))
                .build());

        portRequestServer.start();

        StateChangeListener portRequesterCloser = new StateChangeListener() {
            @Override
            public void stateChanged(HState oldState, HState newState) {
                if (oldState == HState.WAITING_FOR_CLIENT || newState == HState.NOT_CONNECTED) {
                    hConnection.getStateObservable().removeListener(this);
                    try {
                        portRequestServer.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        hConnection.getStateObservable().addListener(portRequesterCloser);
    }

    @Override
    public void stateChanged(HState oldState, HState newState) {
        if (oldState == HState.WAITING_FOR_CLIENT && newState == HState.CONNECTED) {
            // Unregister but do not stop http proxy.
            // We are not stopping the http proxy itself because the hotel websocket is connected to it.
            httpProxy.pause();
            LOG.info("Unity proxy paused");
        }
    }
}
