package gearth.protocol.connection.proxy;

import gearth.misc.Cacher;
import gearth.misc.OSValidator;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HStateSetter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProxyProviderFactory {

    public static final String HOTELS_CACHE_KEY = "hotelsConnectionInfo";

    public static List<String> autoDetectHosts;
    static {
        autoDetectHosts = new ArrayList<>();
        autoDetectHosts.add("game-br.habbo.com:30000");
        autoDetectHosts.add("game-de.habbo.com:30000");
        autoDetectHosts.add("game-es.habbo.com:30000");
        autoDetectHosts.add("game-fi.habbo.com:30000");
        autoDetectHosts.add("game-fr.habbo.com:30000");
        autoDetectHosts.add("game-it.habbo.com:30000");
        autoDetectHosts.add("game-nl.habbo.com:30000");
        autoDetectHosts.add("game-tr.habbo.com:30000");
        autoDetectHosts.add("game-us.habbo.com:38101");

        List<Object> additionalCachedHotels = Cacher.getList(HOTELS_CACHE_KEY);
        if (additionalCachedHotels != null) {
            for (Object additionalHotel : additionalCachedHotels) {
                if (!autoDetectHosts.contains(additionalHotel)) {
                    autoDetectHosts.add((String)additionalHotel);
                }
            }
        }

        if (OSValidator.isMac()) {
            for (int i = 2; i <= autoDetectHosts.size() + 5; i++) {
                ProcessBuilder allowLocalHost = new ProcessBuilder("ifconfig", "lo0", "alias", ("127.0.0." + i), "up");
                try {
                    allowLocalHost.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection hConnection;

    public ProxyProviderFactory(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection){
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.hConnection = hConnection;
    }

    // checks if host is a raw IP instead of a domain
    // TODO support ipv6 (not only here, also in IPmapper)
    static boolean hostIsIpAddress(String host){
        for (char c : host.toCharArray()) {
            if (c != '.' && (c < '0' || c > '9')) {
                return false;
            }
        }
        return true;
    }

    public ProxyProvider provide()  {
        return provide(autoDetectHosts);
    }

    public ProxyProvider provide(String domain, int port)  {
        List<Object> additionalCachedHotels = Cacher.getList(HOTELS_CACHE_KEY);
        if (additionalCachedHotels == null) {
            additionalCachedHotels = new ArrayList<>();
        }
        if (!additionalCachedHotels.contains(domain +":"+port)) {
            additionalCachedHotels.add(domain+":"+port);
            Cacher.put(HOTELS_CACHE_KEY, additionalCachedHotels);
        }

        if (hostIsIpAddress(domain)) {
            SocksConfiguration config = SocksProxyProvider.getSocksConfig();
            if (RawIpProxyProvider.isNoneConnected(domain) &&
                    (!config.useSocks() || config.dontUseFirstTime()) ) {
                return new RawIpProxyProvider(proxySetter, stateSetter, hConnection, domain, port);
            }
            else if (config.useSocks()) {
                return new SocksProxyProvider(proxySetter, stateSetter, hConnection, domain, port);
            }
            return null;
        }
        else {
            List<String> potentialHost = new ArrayList<>();
            potentialHost.add(domain+":"+port);
            return provide(potentialHost);
        }
    }

    private ProxyProvider provide(List<String> potentialHosts) {
        return new NormalProxyProvider(proxySetter, stateSetter, hConnection, potentialHosts);
    }
}
