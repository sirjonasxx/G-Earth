package gearth.services.internal_extensions.extensionstore.application.entities.installed;

import gearth.GEarth;
import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.services.internal_extensions.extensionstore.application.WebUtils;
import gearth.services.internal_extensions.extensionstore.application.entities.StoreExtensionItem;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.services.internal_extensions.extensionstore.tools.InstalledExtension;
import org.apache.maven.artifact.versioning.ComparableVersion;

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
    protected String displayColor(int i) {
        if (storeExtension != null) {
            if (new ComparableVersion(this.installedExtension.getVersion()).compareTo(new ComparableVersion(storeExtension.getVersion())) < 0) {
                return "item_orange";
            }
            return super.displayColor(i);
        }
        return "item_red";
    }

    @Override
    protected String displayVersion() {
        return installedExtension.getVersion();
    }


    @Override
    public void addHtml(int i, GExtensionStore gExtensionStore) {
        if (this.storeExtension != null) {
            super.addHtml(i, gExtensionStore);
        }
        else {

            // display a red item that can't be clicked - to mark that this is no available extension
            StringBuilder htmlBuilder = new StringBuilder()
                    .append("<div class=\"overview_item ").append(displayColor(i)).append(" content_item\">")

                    .append("<div class=\"overview_item_logo\">")
                    .append("<img src=\"\" alt=\"\">")
                    .append("</div>")

                    .append("<div class=\"overview_item_info\">")
                    .append("<div class=\"oii_name\">").append(WebUtils.escapeMessage(installedExtension.getName())).append("</div>")
                    .append("<div class=\"oii_desc\">").append(GEarth.translation.getString("ext.store.extension.notinstore")).append("</div>")
                    .append("</div>")

                    .append("<div class=\"overview_item_msgs\">")
                    .append("<div class=\"oim_top\">").append(GEarth.translation.getString("ext.store.extension.version")).append(": ").append(displayVersion()).append("</div>")
                    .append("<div class=\"oim_bottom\"></div>")
                    .append("</div>")

                    .append("</div>");

            String extension = htmlBuilder.toString();
            GExtensionStoreController controller = gExtensionStore.getController();

            controller.getWebView().getEngine().executeScript("document.getElementById('" +
                    controller.getContentItemsContainer() + "').innerHTML += '" + extension + "';");
        }
    }
}
