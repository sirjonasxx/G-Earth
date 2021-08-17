package gearth.services.internal_extensions.extensionstore.repository.models;

import java.util.List;

public class StoreData {

    private final List<ExtCategory> categories;
    private final List<ExtFramework> frameworks;

    private final List<StoreExtension> extensions;

    public StoreData(List<ExtCategory> categories, List<ExtFramework> frameworks, List<StoreExtension> extensions) {
        this.categories = categories;
        this.frameworks = frameworks;
        this.extensions = extensions;
    }

    public List<ExtCategory> getCategories() {
        return categories;
    }

    public List<ExtFramework> getFrameworks() {
        return frameworks;
    }

    public List<StoreExtension> getExtensions() {
        return extensions;
    }
}
