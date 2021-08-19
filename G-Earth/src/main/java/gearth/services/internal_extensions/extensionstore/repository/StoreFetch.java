package gearth.services.internal_extensions.extensionstore.repository;

import gearth.Main;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreData;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.nio.charset.StandardCharsets;

public class StoreFetch {

    public interface StoreFetchListener {

        void success(StoreRepository storeRepository);
        void fail(String reason);

    }

    public static void fetch(StoreFetchListener storeFetchListener) {
        fetch(Main.version, storeFetchListener, "sirjonasxx/G-ExtensionStore");
    }

    public static void fetch(String version, StoreFetchListener storeFetchListener) {
        fetch(version, storeFetchListener, "sirjonasxx/G-ExtensionStore");
    }

    public static void fetch(String version, StoreFetchListener storeFetchListener, String source) {

        new Thread(() -> {
            try {
                JSONObject config = new JSONObject(IOUtils.toString(
                        new URL(String.format("https://raw.githubusercontent.com/%s/repo/%s/store/config.json", source, version))
                                .openStream(), StandardCharsets.UTF_8));

                JSONArray exensions = new JSONArray(IOUtils.toString(
                        new URL(String.format("https://raw.githubusercontent.com/%s/repo/%s/.auto-generated/extensions.json", source, version))
                                .openStream(), StandardCharsets.UTF_8));

                storeFetchListener.success(new StoreRepository(new StoreData(config, exensions), version, source));

            } catch (Exception e) {
                storeFetchListener.fail(e.getLocalizedMessage());
            }
        }).start();

    }


    public static void main(String[] args) {
//        fetch("1.4.1");
    }

}
