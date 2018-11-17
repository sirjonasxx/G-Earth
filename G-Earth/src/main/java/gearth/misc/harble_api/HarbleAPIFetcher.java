package gearth.misc.harble_api;

import gearth.misc.Cacher;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Created by Jonas on 10/11/2018.
 */
public class HarbleAPIFetcher {

    public static final String CACHE_PREFIX = "HARBLE_API-";
    public static final String HARBLE_API_URL = "https://api.harble.net/revisions/$hotelversion$.json";

    public static HarbleAPI fetch(String hotelversion) {
        String cacheName = CACHE_PREFIX + hotelversion;

        if (Cacher.cacheFileExists(cacheName)) {
            return new HarbleAPI(hotelversion);
        }
        else {
            Connection connection = Jsoup.connect(HARBLE_API_URL.replace("$hotelversion$", hotelversion)).ignoreContentType(true);
            try {
                Document doc = connection.get();
                Connection.Response response = connection.response();
                if (response.statusCode() == 200) {
                    String s = doc.body().toString();
                    s = s.substring(6, s.length() - 7);
                    JSONObject object = new JSONObject(s);
                    Cacher.updateCache(object, cacheName);

                    return new HarbleAPI(hotelversion);
                }
                else {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }

        }
    }

    public static void main(String[] args) {
        HarbleAPI api = fetch("PRODUCTION-201810171204-70166177");
    }
}