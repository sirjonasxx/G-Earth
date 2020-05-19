package gearth.protocol.connection.proxy;

import gearth.misc.Cacher;
import gearth.misc.OSValidator;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.unix.LinuxRawIpProxyProvider;
import gearth.protocol.connection.proxy.unix.LinuxRawIpSocksProxyProvider;
import gearth.protocol.connection.proxy.windows.WindowsRawIpProxyProvider;
import gearth.protocol.connection.proxy.windows.WindowsRawIpSocksProxyProvider;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProxyProviderFactory {

    public static final String HOTELS_CACHE_KEY = "hotelsConnectionInfo";
    private static SocksConfiguration socksConfig = null;

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
            if (OSValidator.isWindows()) {
                if (WindowsRawIpProxyProvider.isNoneConnected(domain) &&
                        (!socksConfig.useSocks() || socksConfig.dontUseFirstTime()) ) {
                    return new WindowsRawIpProxyProvider(proxySetter, stateSetter, hConnection, domain, port);
                }
                else if (socksConfig.useSocks()) {
                    return new WindowsRawIpSocksProxyProvider(proxySetter, stateSetter, hConnection, domain, port);
                }

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "G-Earth is already connected to this hotel. " +
                            "Due to current limitations you can only connect one session per hotel to G-Earth in Raw IP mode on Windows.\n\n" +
                            "You can bypass this by using a SOCKS proxy [Extra -> Advanced -> SOCKS]", ButtonType.OK);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.setResizable(false);
                    alert.show();
                });

                return null;
            }
            else if (OSValidator.isUnix()) {
                if (!socksConfig.useSocks() || socksConfig.dontUseFirstTime()) {
                    return new LinuxRawIpProxyProvider(proxySetter, stateSetter, hConnection, domain, port);
                }
                else {
                    return new LinuxRawIpSocksProxyProvider(proxySetter, stateSetter, hConnection, domain, port);
                }
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

    public static void setSocksConfig(SocksConfiguration configuration) {
        socksConfig = configuration;
    }

    public static SocksConfiguration getSocksConfig() {
        return socksConfig;
    }
}
