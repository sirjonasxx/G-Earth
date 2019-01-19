package gearth.misc.harble_api;

import gearth.misc.Cacher;
import gearth.protocol.HMessage;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Created by Jonas on 10/11/2018.
 */

/**
 * Ok the usage of this class is pretty shitty so I'm just gonna add some documentation here
 *
 * What this class does is fetching the revision (if needed) from the API, this is the only class with communication with the
 * actual API. Then the result (if any) gets cached.
 *
 * The method "fetch(xxx);" needs to be called exactly once at the moment a new connection has been made.
 *
 * However, at that same moment the Extension class needs to send the "startConnection" signal to the extensions, and we want to make sure
 * that the cached revision is already available at the moment the extensions get initialized with a new connection. That's why the
 * fetch() method here only gets called by the extension class as that's the only way to ensure this method gets called BEFORE the extensions
 * start. (bc im lazy and dont wanna rewrite code too)
 *
 *
 * the "HARBLEAPI" object contains the latest fetched object and is ensured to be up-to-date with the current connection
 */
public class HarbleAPIFetcher {

    public static final String CACHE_PREFIX = "HARBLE_API-";
    public static final String HARBLE_API_URL = "https://api.harble.net/messages/$hotelversion$.json";

    //latest fetched
    public static HarbleAPI HARBLEAPI = null;

    public synchronized static void fetch(String hotelversion) {
        String cacheName = CACHE_PREFIX + hotelversion;

        if (Cacher.cacheFileExists(cacheName)) {
            HARBLEAPI = new HarbleAPI(hotelversion);
        }
        else {
            Connection connection = Jsoup.connect(HARBLE_API_URL.replace("$hotelversion$", hotelversion)).ignoreContentType(true);
            try {
                Connection.Response response = connection.execute();
                if (response.statusCode() == 200) {
                    String messagesBodyJson = response.body();
                    Cacher.updateCache(messagesBodyJson, cacheName);
                    HARBLEAPI = new HarbleAPI(hotelversion);
                }
                else {
                    HARBLEAPI = null;
                }
            } catch (IOException e) {
                HARBLEAPI = null;
            }

        }
    }

    public static void main(String[] args) {
        fetch("PRODUCTION-201901141210-114421986");

        HarbleAPI api = HARBLEAPI;
        HarbleAPI.HarbleMessage haMessage = api.getHarbleMessageFromHeaderId(HMessage.Side.TOSERVER, 525);
        System.out.println(haMessage);
    }
}