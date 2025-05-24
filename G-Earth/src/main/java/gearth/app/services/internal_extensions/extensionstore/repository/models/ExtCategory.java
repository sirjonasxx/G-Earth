package gearth.app.services.internal_extensions.extensionstore.repository.models;

import org.json.JSONObject;

public class ExtCategory {

    private final String name;
    private final String description;
    private final String icon;

    public ExtCategory(String name, String description, String icon) {
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public ExtCategory(JSONObject object) {
        this.name = object.getString("name");
        this.description = object.getString("description");
        this.icon = object.getString("icon");
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }
}
