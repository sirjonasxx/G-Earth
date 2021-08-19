package gearth.services.internal_extensions.extensionstore.application.entities.extensiondetails;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;

public class StoreExtensionDetailsItem implements ContentItem {

    private final StoreExtension storeExtension;

    public StoreExtensionDetailsItem(StoreExtension storeExtension) {
        this.storeExtension = storeExtension;
    }

    @Override
    public void addHtml(int i, GExtensionStore gExtensionStore) {

    }
}
