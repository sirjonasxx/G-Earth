package gearth.app.services.internal_extensions.extensionstore.repository.models;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StoreConfig {

    private final List<ExtCategory> categories;
    private final List<ExtFramework> frameworks;

    public StoreConfig(List<ExtCategory> categories, List<ExtFramework> frameworks) {
        this.categories = categories;
        this.frameworks = frameworks;
    }

    public StoreConfig(JSONObject object) {
        this.categories = object.getJSONArray("categories").toList().stream().map(o -> new ExtCategory(new JSONObject((Map)o))).collect(Collectors.toList());
        this.frameworks = object.getJSONArray("frameworks").toList().stream().map(o -> new ExtFramework(new JSONObject((Map)o))).collect(Collectors.toList());
    }

    public List<ExtCategory> getCategories() {
        return categories;
    }

    public List<ExtFramework> getFrameworks() {
        return frameworks;
    }
}
