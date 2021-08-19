package gearth.services.internal_extensions.extensionstore.application.entities.categories;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.repository.models.ExtCategory;

public class CategoryItem implements ContentItem {

    private final ExtCategory category;

    public CategoryItem(ExtCategory category) {
        this.category = category;
    }

    @Override
    public void addHtml(int i, GExtensionStore gExtensionStore) {

    }
}
