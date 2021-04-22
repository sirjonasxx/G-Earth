package gearth.misc.harble_api;

import gearth.Main;
import gearth.misc.Cacher;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;


public class HarbleAPIFetcher {

    public static final String CACHE_PREFIX = "HARBLE_API-";
    public static final String HARBLE_API_URL = "https://api.harble.net/messages/$hotelversion$.json";

    //latest fetched
    public static PacketInfoManager HARBLEAPI = null;

    public synchronized static void fetch(String hotelversion, String clientType) {
        // if unity
        if (clientType.toLowerCase().contains("unity")) {
            try {
                HARBLEAPI = new PacketInfoManager(
                        new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                                .getParentFile(), "messages.json"
                        )
                );
            } catch (URISyntaxException e) {
                HARBLEAPI = null;
            }
            return;
        }

        String cacheName = CACHE_PREFIX + hotelversion;

        if (Cacher.cacheFileExists(cacheName)) {
            HARBLEAPI = new PacketInfoManager(hotelversion);
        }
        else {
            Connection connection = Jsoup.connect(HARBLE_API_URL.replace("$hotelversion$", hotelversion)).ignoreContentType(true);
            try {
                connection.timeout(3000);
                Connection.Response response = connection.execute();
                if (response.statusCode() == 200) {
                    String messagesBodyJson = response.body();
                    Cacher.updateCache(messagesBodyJson, cacheName);
                    HARBLEAPI = new PacketInfoManager(hotelversion);
                }
                else {
                    HARBLEAPI = null;
                }
            } catch (IOException e) {
                HARBLEAPI = null;
            }

        }
    }
}