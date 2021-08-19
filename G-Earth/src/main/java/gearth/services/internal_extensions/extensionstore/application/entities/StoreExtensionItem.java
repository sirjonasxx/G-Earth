package gearth.services.internal_extensions.extensionstore.application.entities;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;

public class StoreExtensionItem implements ContentItem {

    protected final StoreExtension storeExtension;

    public StoreExtensionItem(StoreExtension storeExtension) {
        this.storeExtension = storeExtension;
    }

    @Override
    public void addHtml(int i, GExtensionStore gExtensionStore) {

    }

}
