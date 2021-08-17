package gearth.services.internal_extensions.extensionstore.repository.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StoreData {

    private final StoreConfig config;
    private final List<StoreExtension> extensions;

    public StoreData(StoreConfig config, List<StoreExtension> extensions) {
        this.config = config;
        this.extensions = extensions;
    }

    public StoreData(JSONObject config, JSONArray extensions) {
        this.config = new StoreConfig(config);
        this.extensions = extensions.toList().stream().map(o -> new StoreExtension(new JSONObject((Map)o), this.config)).collect(Collectors.toList());
    }

    public List<StoreExtension> getExtensions() {
        return extensions;
    }

    public StoreConfig getConfig() {
        return config;
    }
}
