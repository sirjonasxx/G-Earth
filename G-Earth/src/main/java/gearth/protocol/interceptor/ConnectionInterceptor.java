package gearth.protocol.interceptor;

import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HClient;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.SocksConfiguration;
import gearth.protocol.hostreplacer.hostsfile.HostReplacer;
import gearth.protocol.hostreplacer.hostsfile.HostReplacerFactory;
import gearth.protocol.portchecker.PortChecker;
import gearth.protocol.portchecker.PortCheckerFactory;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionInterceptor.class);
    private static final HostReplacer hostsReplacer = HostReplacerFactory.get();

    private final HClient hClient;
    private final HStateSetter hStateSetter;
    private final HConnection hConnection;
    private final ConnectionInterceptorCallbacks callbacks;
    private final List<String> potentialHosts;

    private volatile boolean hostRedirected = false;

    private volatile List<HProxy> potentialProxies = new ArrayList<>();
    private volatile HProxy proxy = null;

    private boolean useSocks;

    public ConnectionInterceptor(HClient client, HStateSetter stateSetter, HConnection hConnection, ConnectionInterceptorCallbacks callbacks, List<String> potentialHosts, boolean useSocks) {
        this.hClient = client;
        this.hStateSetter = stateSetter;
        this.hConnection = hConnection;
        this.callbacks = callbacks;
        this.potentialHosts = potentialHosts;
        this.useSocks = useSocks;
    }

    public void start() throws IOException {
        prepare();

        if (!addToHosts()) {
            Platform.runLater(() -> {
                try {
                    final Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to modify hosts file, try to run G-Earth as administrator.", ButtonType.OK);

                    alert.setHeaderText("Error modifying hosts file");

                    TitleBarController
                            .create(alert)
                            .showAlertAndWait();
                } catch (IOException ex) {
                    logger.error("Error showing hosts error alert", ex);
                }
            });

            hStateSetter.setState(HState.NOT_CONNECTED);
            return;
        }

        launchProxy();
    }

    public void stop(boolean forceRemoveFromHosts) {
        if (forceRemoveFromHosts || hostRedirected) {
            removeFromHosts();
        }

        clearAllProxies();
    }

    private void prepare() {
        hStateSetter.setState(HState.PREPARING);

        List<String> willremove = new ArrayList<>();
        int c = 0;
        for (String host : potentialHosts) {
            String[] split = host.split(":");
            String input_dom = split[0];
            if (!ProxyProviderFactory.hostIsIpAddress(input_dom)) {
                int port = Integer.parseInt(split[1]);
                String actual_dom;

                InetAddress address;
                try {
                    address = InetAddress.getByName(input_dom);
                    actual_dom = address.getHostAddress();
                }
                catch (UnknownHostException e) {
                    willremove.add(host);
                    continue;
                }

                int intercept_port = port;
                String intercept_host = "127.0." + (c / 254) + "." + (1 + c % 254);
                potentialProxies.add(new HProxy(hClient, input_dom, actual_dom, port, intercept_port, intercept_host));
                c++;
            }
        }

        List<Object> additionalCachedHotels = Cacher.getList(ProxyProviderFactory.HOTELS_CACHE_KEY);
        if (additionalCachedHotels != null) {
            for (String host : willremove) {
                additionalCachedHotels.remove(host);
            }
            Cacher.put(ProxyProviderFactory.HOTELS_CACHE_KEY, additionalCachedHotels);
        }

        hStateSetter.setState(HState.PREPARED);
    }

    private void launchProxy() throws IOException {
        hStateSetter.setState(HState.WAITING_FOR_CLIENT);

        for (int c = 0; c < potentialProxies.size(); c++) {
            HProxy potentialProxy = potentialProxies.get(c);

            ServerSocket proxy_server;
            try {
                proxy_server = new ServerSocket(potentialProxy.getIntercept_port(), 10, InetAddress.getByName(potentialProxy.getIntercept_host()));
            } catch (BindException e) {
                PortChecker portChecker = PortCheckerFactory.getPortChecker();
                String processName = portChecker.getProcessUsingPort(potentialProxy.getIntercept_port());
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, "The port is in use by " + processName,
                            ButtonType.OK);
                    try {
                        TitleBarController.create(a).showAlertAndWait();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
                throw new IOException(e);
            }
            potentialProxy.initProxy(proxy_server);

            new Thread(() -> {
                try  {
                    Thread.sleep(30);
                    while ((hConnection.getState() == HState.WAITING_FOR_CLIENT) && !proxy_server.isClosed())	{
                        try {
                            Socket client = proxy_server.accept();
                            proxy = potentialProxy;
                            closeAllProxies(proxy);
                            if (HConnection.DEBUG) logger.debug("Accepted a proxy");

                            new Thread(() -> {
                                try {
                                    Socket server;
                                    if (!useSocks) {
                                        try {
                                            server = new Socket(proxy.getActual_domain(), proxy.getActual_port());
                                        } catch (Exception e) {
                                            logger.error("Failed to connect to Habbo server {}:{}", proxy.getActual_domain(), proxy.getActual_port());
                                            callbacks.onInterceptorError();
                                            return;
                                        }
                                    }
                                    else {
                                        SocksConfiguration configuration = ProxyProviderFactory.getSocksConfig();
                                        if (configuration == null) {
                                            callbacks.onInterceptorError();
                                            return;
                                        }
                                        server = configuration.createSocket();
                                        server.connect(new InetSocketAddress(proxy.getActual_domain(), proxy.getActual_port()), 5000);
                                    }

                                    callbacks.onInterceptorConnected(client, server, proxy);
                                } catch (Exception e) {
                                    logger.error("Error occurred while intercepting connection", e);
                                    callbacks.onInterceptorError();
                                }
                            }).start();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                } catch (Exception e) {
                    logger.error("Proxy server thread error", e);
                }
            }).start();
        }


        if (HConnection.DEBUG) System.out.println("done waiting for clients with: " + hConnection.getState() );

    }

    private boolean addToHosts() {
        List<String> linesTemp = new ArrayList<>();
        for (HProxy proxy : potentialProxies) {
            linesTemp.add(proxy.getIntercept_host() + " " + proxy.getInput_domain());
        }

        String[] lines = new String[linesTemp.size()];
        for (int i = 0; i < linesTemp.size(); i++) {
            lines[i] = linesTemp.get(i);
        }

        if (hostsReplacer.addRedirect(lines)) {
            hostRedirected = true;
            return true;
        }

        return false;
    }

    private boolean removeFromHosts(){
        List<String> linesTemp = new ArrayList<>();
        for (HProxy proxy : potentialProxies) {
            linesTemp.add(proxy.getIntercept_host() + " " + proxy.getInput_domain());
        }

        String[] lines = new String[linesTemp.size()];
        for (int i = 0; i < linesTemp.size(); i++) {
            lines[i] = linesTemp.get(i);
        }

        if (hostsReplacer.removeRedirect(lines)) {
            hostRedirected = false;
            return true;
        }

        return false;
    }

    private void clearAllProxies() {
        closeAllProxies(null);
//        potentialProxies = new ArrayList<>();
    }

    private void closeAllProxies(HProxy except) {
        for (HProxy proxy : potentialProxies) {
            if (except != proxy) {
                if (proxy.getProxy_server() != null && !proxy.getProxy_server().isClosed())	{
                    try {
                        proxy.getProxy_server().close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
//        potentialProxies = Collections.singletonList(except);
    }

}
