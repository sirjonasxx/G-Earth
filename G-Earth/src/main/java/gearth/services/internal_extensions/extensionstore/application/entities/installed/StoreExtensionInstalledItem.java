package gearth.services.internal_extensions.extensionstore.application.entities.installed;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.entities.StoreExtensionItem;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.services.internal_extensions.extensionstore.tools.InstalledExtension;

public class StoreExtensionInstalledItem extends StoreExtensionItem {

    // color red when removed
    // color orange when needs update
    // otherwise normal color

    private final InstalledExtension installedExtension;

    public StoreExtensionInstalledItem(StoreExtension storeExtension, InstalledExtension installedExtension) {
        super(storeExtension);
        this.installedExtension = installedExtension;
    }

    @Override
    public void addHtml(int i, GExtensionStore gExtensionStore) {
        if (this.storeExtension != null) {
            super.addHtml(i, gExtensionStore /* add custom color here */);
        }


        //todo
    }
}
