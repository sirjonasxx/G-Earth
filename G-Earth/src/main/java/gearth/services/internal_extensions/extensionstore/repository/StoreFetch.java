package gearth.services.internal_extensions.extensionstore.repository;

import gearth.services.internal_extensions.extensionstore.repository.models.StoreConfig;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreData;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class StoreFetch {

    public interface StoreFetchListener {

        void success(StoreRepository storeRepository);
        void fail(String reason);

    }

    public static void fetch(String version, StoreFetchListener storeFetchListener) {

        new Thread(() -> {
            try {
                JSONObject config = new JSONObject(IOUtils.toString(
                        new URL(String.format("https://raw.githubusercontent.com/sirjonasxx/G-ExtensionStore/repo/%s/store/config.json", version))
                                .openStream(), StandardCharsets.UTF_8));

                JSONArray exensions = new JSONArray(IOUtils.toString(
                        new URL(String.format("https://raw.githubusercontent.com/sirjonasxx/G-ExtensionStore/repo/%s/.auto-generated/extensions.json", version))
                                .openStream(), StandardCharsets.UTF_8));

                storeFetchListener.success(new StoreRepository(new StoreData(config, exensions), version));

            } catch (Exception e) {
                storeFetchListener.fail(e.getLocalizedMessage());
            }
        }).start();

    }


    public static void main(String[] args) {
//        get("1.4.1");
    }

}
