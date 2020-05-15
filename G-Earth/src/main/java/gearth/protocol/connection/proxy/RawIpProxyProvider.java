package gearth.protocol.connection.proxy;

import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.hostreplacer.ipmapping.IpMapper;
import gearth.protocol.hostreplacer.ipmapping.IpMapperFactory;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class RawIpProxyProvider extends ProxyProvider {

    private volatile String input_host;
    private volatile int input_port;

    private IpMapper ipMapper = IpMapperFactory.get();
    private boolean hasMapped = false;

    protected HProxy proxy = null;

    public RawIpProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, String input_host, int input_port) {
        super(proxySetter, stateSetter, hConnection);
        this.input_host = input_host;
        this.input_port = input_port;
    }

    @Override
    public void start() {
        if (hConnection.getState() != HState.NOT_CONNECTED) {
            return;
        }

        launchMITM();
    }

    private void launchMITM() {
        new Thread(() -> {
            try  {
                stateSetter.setState(HState.PREPARING);
                proxy = new HProxy(input_host, input_host, input_port, input_port, "0.0.0.0");

                maybeRemoveMapping();

                if (!onBeforeIpMapping()) {
                    stateSetter.setState(HState.NOT_CONNECTED);
                    return;
                }

                maybeAddMapping();

                if (HConnection.DEBUG) System.out.println("Added mapping for raw IP");

                ServerSocket proxy_server = new ServerSocket(proxy.getIntercept_port(), 10, InetAddress.getByName(proxy.getIntercept_host()));
                proxy.initProxy(proxy_server);

                stateSetter.setState(HState.WAITING_FOR_CLIENT);
                while ((hConnection.getState() == HState.WAITING_FOR_CLIENT) && !proxy_server.isClosed())	{
                    try {
                        if (HConnection.DEBUG) System.out.println("try accept proxy");
                        Socket client = proxy_server.accept();

                        if (HConnection.DEBUG) System.out.println("accepted a proxy");

                        new Thread(() -> {
                            try {
                                createProxyThread(client);
                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                        }).start();

                    } catch (IOException ignored) {
                    }
                }



            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void abort() {
        stateSetter.setState(HState.ABORTING);
        if (hasMapped) {
            maybeRemoveMapping();
        }
        tryCloseProxy();
        super.abort();
    }

    @Override
    protected void onConnect() {
        super.onConnect();
        tryCloseProxy();
    }

    @Override
    protected void onConnectEnd() {
        if (hasMapped) {
            maybeRemoveMapping();
        }
        tryCloseProxy();
        super.onConnectEnd();
    }

    private void tryCloseProxy() {
        if (proxy.getProxy_server() != null && !proxy.getProxy_server().isClosed())	{
            try {
                proxy.getProxy_server().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Queue<Socket> preConnectedServerConnections;

    // returns false if fail
    protected boolean onBeforeIpMapping() throws IOException, InterruptedException {
        preConnectedServerConnections = new LinkedList<>();
        for (int i = 0; i < 3; i++) {
            Socket s1 = new Socket();
            s1.setSoTimeout(1200);
            try {
                s1.connect(new InetSocketAddress(proxy.getActual_domain(), proxy.getActual_port()), 1200);
            }
            catch (SocketTimeoutException e) {
                showInvalidConnectionError();
                return false;
            }

            preConnectedServerConnections.add(s1);
            Thread.sleep(50);
        }

        return true;
    }

    protected void createProxyThread(Socket client) throws IOException, InterruptedException {
        if (preConnectedServerConnections.isEmpty()) {
            if (HConnection.DEBUG) System.out.println("pre-made server connections ran out of stock");
        }
        else {
            startProxyThread(client, preConnectedServerConnections.poll(), proxy);
        }
    }

    protected void showInvalidConnectionError() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "You entered invalid connection information, G-Earth could not connect", ButtonType.OK);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.setResizable(false);
            alert.show();
        });
    }


    private void maybeAddMapping() {
        if (!hasMapped) {
            hasMapped = true;
            if (isNoneConnected()) {
                ipMapper.enable();
                ipMapper.addMapping(proxy.getActual_domain());
            }
            addMappingCache();
        }

    }

    protected void maybeRemoveMapping() {
        if (hasMapped) {
            hasMapped = false;
            removeMappingCache();
            if (isNoneConnected()) {
                ipMapper.deleteMapping(proxy.getActual_domain());
            }
        }
    }



    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final String RAWIP_CONNECTIONS = "rawip_connections";


    // let other G-Earth instances know you're connected
    private void addMappingCache() {
        new Thread(() -> {
            while (hasMapped) {
                updateMappingCache();
                try {
                    Thread.sleep(55000);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    // checks if no G-Earth instances are connected
    // every G-Earth instance is supposed to update if it's still connected every 60 seconds
    private boolean isNoneConnected() {
        return isNoneConnected(proxy.getActual_domain());
    }

    private void updateMappingCache() {
        JSONObject connections = getCurrentConnectionsCache();

        JSONObject instance = new JSONObject();
        instance.put("timestamp", BigInteger.valueOf(System.currentTimeMillis()));

        connections.put(INSTANCE_ID.toString(), instance);
        saveCurrentConnectionsCache(connections);
    }

    private void removeMappingCache() {
        JSONObject connections = getCurrentConnectionsCache();
        connections.remove(INSTANCE_ID.toString());
        saveCurrentConnectionsCache(connections);
    }

    private JSONObject getCurrentConnectionsCache() {
        return getCurrentConnectionsCache(proxy.getActual_domain());
    }

    private void saveCurrentConnectionsCache(JSONObject connections) {
        if (!Cacher.getCacheContents().has(proxy.getActual_domain())) {
            Cacher.put(RAWIP_CONNECTIONS, new JSONObject());
        }
        JSONObject gearthConnections = Cacher.getCacheContents().getJSONObject(RAWIP_CONNECTIONS);
        gearthConnections.put(proxy.getActual_domain(), connections);
        Cacher.put(RAWIP_CONNECTIONS, gearthConnections);
    }


    static private JSONObject getCurrentConnectionsCache(String actual_host) {
        if (!Cacher.getCacheContents().has(RAWIP_CONNECTIONS)) {
            Cacher.put(RAWIP_CONNECTIONS, new JSONObject());
        }
        JSONObject gearthConnections = Cacher.getCacheContents().getJSONObject(RAWIP_CONNECTIONS);

        if (!gearthConnections.has(actual_host)) {
            gearthConnections.put(actual_host, new JSONObject());
            Cacher.put(RAWIP_CONNECTIONS, gearthConnections);
        }
        return gearthConnections.getJSONObject(actual_host);
    }

    static boolean isNoneConnected(String actual_host) {
        JSONObject connections = getCurrentConnectionsCache(actual_host);

        BigInteger timeoutTimestamp = BigInteger.valueOf(System.currentTimeMillis() - 60000);
        for (String key : connections.keySet()) {
            JSONObject connection = connections.getJSONObject(key);
            if (!key.equals(INSTANCE_ID.toString())) {
                if (connection.getBigInteger("timestamp").compareTo(timeoutTimestamp) > 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
