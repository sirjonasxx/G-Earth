package gearth.protocol.connection.proxy.flash.windows;

import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.flash.unix.LinuxRawIpFlashProxyProvider;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.UUID;

// windows raw ip proxy provider extends the Linux one with the exception that it does not want to close
// the IP redirect on connect
public class WindowsRawIpFlashProxyProvider extends LinuxRawIpFlashProxyProvider {

    private boolean hasMapped = false;

    public WindowsRawIpFlashProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, String input_host, int input_port, boolean useSocks) {
        super(proxySetter, stateSetter, hConnection, input_host, input_port, useSocks);
    }


    @Override
    protected void onConnect() {
        //NOTE: DOES NOT CALL SUPER METHOD

        stateSetter.setState(HState.CONNECTED);
        tryCloseProxy();
    }


    protected void maybeAddMapping() {
        if (!hasMapped) {
            hasMapped = true;
            if (isNoneConnected()) {
                ipMapper.enable();
                ipMapper.addMapping(proxy.getActual_domain(), proxy.getActual_port(), proxy.getIntercept_port());
            }
            addMappingCache();
        }

    }

    protected void maybeRemoveMapping() {
        if (hasMapped) {
            hasMapped = false;
            removeMappingCache();
            if (isNoneConnected()) {
                ipMapper.deleteMapping(proxy.getActual_domain(), proxy.getActual_port(), proxy.getIntercept_port());
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


    private static JSONObject getCurrentConnectionsCache(String actual_host) {
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

    public static boolean isNoneConnected(String actual_host) {
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
