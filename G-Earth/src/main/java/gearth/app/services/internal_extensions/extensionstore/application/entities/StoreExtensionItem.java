package gearth.app.services.internal_extensions.extensionstore.application.entities;

import gearth.app.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.app.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.app.services.internal_extensions.extensionstore.application.WebUtils;
import gearth.app.services.internal_extensions.extensionstore.application.entities.extensiondetails.StoreExtensionDetailsOverview;
import gearth.app.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.app.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.app.ui.translations.LanguageBundle;
import netscape.javascript.JSObject;

public class StoreExtensionItem implements ContentItem {

    protected final StoreExtension storeExtension;
    private GExtensionStore gExtensionStore = null;

    public StoreExtensionItem(StoreExtension storeExtension) {
        this.storeExtension = storeExtension;
    }

    public void onClick() {
        gExtensionStore.getController().pushOverview(
                new StoreExtensionDetailsOverview(
                        gExtensionStore.getController().getCurrentOverview(),
                        0,
                        GExtensionStore.PAGESIZE,
                        storeExtension,
                        gExtensionStore.getRepository()
                )
        );
    }

    protected String displayVersion() {
        return storeExtension.getVersion();
    }

    protected String displayColor(int i) {
        return i % 2 == 0 ? "item_lightblue" : "item_darkblue";
    }

    @Override
    public void addHtml(int i, GExtensionStore gExtensionStore) {
        this.gExtensionStore = gExtensionStore;
        StoreRepository repository = gExtensionStore.getRepository();

        String id = "ext" + i + "_" + System.currentTimeMillis();

        StringBuilder htmlBuilder = new StringBuilder()
                .append("<div class=\"overview_item ").append(displayColor(i)).append(" content_item\">")

                .append("<div class=\"overview_item_logo\">")
                .append("<img src=\"").append(repository.getResourceUrl("store", "extensions", storeExtension.getTitle(), "icon.png")).append("\" alt=\"\" onerror=\"this.src=").append(repository.getResourceUrl("assets", "icons", "placeholder.png")).append("\">")
                .append("</div>")

                .append("<div class=\"overview_item_info\">")
                .append("<div onclick=\"").append(id).append(".onClick()\" class=\"oii_name clickable\">").append(WebUtils.escapeMessage(storeExtension.getTitle())).append("</div>")
                .append("<div class=\"oii_desc\">").append(String.format(LanguageBundle.get("ext.store.extension.madeby"), storeExtension.getAuthors().get(0).getName())).append(", ").append(String.format(LanguageBundle.get("ext.store.extension.lastupdated"), WebUtils.elapsedSince(storeExtension.getUpdateDate()))).append("</div>")
                .append("</div>")

                .append("<div onclick=\"").append(id).append(".onClick()\" class=\"overview_item_msgs clickable\">")
                .append("<div class=\"oim_top\">").append(LanguageBundle.get("ext.store.extension.version")).append(": ").append(displayVersion()).append("</div>")
                .append("<div class=\"oim_bottom\">").append(LanguageBundle.get("ext.store.extension.rating")).append(": ").append(storeExtension.getRating()).append("</div>")
//                .append("<div class=\"oim_bottom\">").append(storeExtension.getFramework().getFramework().getName().replace("Native", "")).append(" </div>")
                .append("</div>")

                .append("</div>");

        String extension = htmlBuilder.toString();
        GExtensionStoreController controller = gExtensionStore.getController();

        controller.getWebView().getEngine().executeScript("document.getElementById('" + controller.getContentItemsContainer() + "').innerHTML += '" + extension + "';");

        JSObject window = (JSObject) controller.getWebView().getEngine().executeScript("window");
        window.setMember(id, this);
    }

}
