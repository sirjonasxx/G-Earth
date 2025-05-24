package gearth.app.protocol.connection.proxy;

import gearth.app.misc.Cacher;
import gearth.app.misc.OSValidator;
import gearth.app.protocol.HConnection;
import gearth.protocol.connection.HClient;
import gearth.app.protocol.connection.HProxySetter;
import gearth.app.protocol.connection.HStateSetter;
import gearth.app.protocol.connection.proxy.flash.FlashProxy;
import gearth.app.protocol.connection.proxy.flash.unix.LinuxRawIpFlashProxyProvider;
import gearth.app.protocol.connection.proxy.flash.windows.WindowsRawIpFlashProxyProvider;
import gearth.app.protocol.connection.proxy.shockwave.ShockwaveProxy;
import gearth.app.ui.titlebar.TitleBarAlert;
import gearth.app.ui.translations.LanguageBundle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProxyProviderFactory {

    public static final String HOTELS_CACHE_KEY = "hotelsConnectionInfo";
    private static SocksConfiguration socksConfig = null;

    private static List<String> autoDetectHosts;
    private static List<String> autoDetectHostsOrigins;
    private static List<String> allHosts;

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
        autoDetectHosts.add("game-us.habbo.com:30000");
        autoDetectHosts.add("game-s2.habbo.com:30000");

        autoDetectHostsOrigins = new ArrayList<>();
        autoDetectHostsOrigins.add("game-od.habbo.com:40001");
        autoDetectHostsOrigins.add("game-ous.habbo.com:40001");
        autoDetectHostsOrigins.add("game-obr.habbo.com:40001");
        autoDetectHostsOrigins.add("game-oes.habbo.com:40001");

        allHosts = new ArrayList<>(autoDetectHosts.size() + autoDetectHostsOrigins.size());
        allHosts.addAll(autoDetectHosts);
        allHosts.addAll(autoDetectHostsOrigins);

        List<Object> additionalCachedHotels = Cacher.getList(HOTELS_CACHE_KEY);
        if (additionalCachedHotels != null) {
            for (Object additionalHotel : additionalCachedHotels) {
                if (!autoDetectHosts.contains(additionalHotel)) {
                    autoDetectHosts.add((String)additionalHotel);
                }
            }
        }

        if (OSValidator.isMac()) {
            for (int i = 2; i <= allHosts.size() + 5; i++) {
                ProcessBuilder allowLocalHost = new ProcessBuilder("ifconfig", "lo0", "alias", ("127.0.0." + i), "up");
                try {
                    allowLocalHost.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<String> getAllHosts() {
        return allHosts;
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
    public static boolean hostIsIpAddress(String host){
        for (char c : host.toCharArray()) {
            if (c != '.' && (c < '0' || c > '9')) {
                return false;
            }
        }
        return true;
    }

    public ProxyProvider provide(HClient client)  {
        return provide(client, client == HClient.SHOCKWAVE ? autoDetectHostsOrigins : autoDetectHosts);
    }

    public ProxyProvider provide(HClient client, String domain, int port)  {
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
                if (WindowsRawIpFlashProxyProvider.isNoneConnected(domain) &&
                        (!socksConfig.useSocks() || socksConfig.onlyUseIfNeeded()) ) {
                    return new WindowsRawIpFlashProxyProvider(proxySetter, stateSetter, hConnection, domain, port, false);
                }
                else if (socksConfig.useSocks()) {
                    return new WindowsRawIpFlashProxyProvider(proxySetter, stateSetter, hConnection, domain, port, true);
                }

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
                    alert.getDialogPane().getChildren().add(new Label(LanguageBundle.get("alert.alreadyconnected.content").replaceAll("\\\\n", System.lineSeparator())));
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.setResizable(false);
                    try {
                        TitleBarAlert.create(alert).showAlert();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                return null;
            }
            else if (OSValidator.isUnix() || OSValidator.isMac()) {
                return new LinuxRawIpFlashProxyProvider(proxySetter, stateSetter, hConnection, domain, port, socksConfig.useSocks() && !socksConfig.onlyUseIfNeeded());
            }
            return null;

        }
        else {
            List<String> potentialHost = new ArrayList<>();
            potentialHost.add(domain+":"+port);
            return provide(client, potentialHost);
        }
    }

    private ProxyProvider provide(HClient client, List<String> potentialHosts) {
        return client == HClient.SHOCKWAVE
                ? new ShockwaveProxy(proxySetter, stateSetter, hConnection, potentialHosts)
                : new FlashProxy(proxySetter, stateSetter, hConnection, potentialHosts, socksConfig.useSocks() && !socksConfig.onlyUseIfNeeded());
    }

    public static void setSocksConfig(SocksConfiguration configuration) {
        socksConfig = configuration;
    }

    public static SocksConfiguration getSocksConfig() {
        return socksConfig;
    }
}
