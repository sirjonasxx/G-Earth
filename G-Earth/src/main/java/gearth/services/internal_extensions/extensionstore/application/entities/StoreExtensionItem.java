package gearth.services.internal_extensions.extensionstore.application.entities;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.services.internal_extensions.extensionstore.application.WebUtils;
import gearth.services.internal_extensions.extensionstore.application.entities.extensiondetails.StoreExtensionDetailsOverview;
import gearth.services.internal_extensions.extensionstore.application.entities.queriedoverviews.CategorizedOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
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
                .append("<img src=\"").append(repository.getResourceUrl(String.format("store/extensions/%s/icon.png", storeExtension.getTitle()))).append("\" alt=\"\">")
                .append("</div>")

                .append("<div class=\"overview_item_info\">")
                .append("<div onclick=\"").append(id).append(".onClick()\" class=\"oii_name clickable\">").append(WebUtils.escapeMessage(storeExtension.getTitle())).append("</div>")
                .append("<div class=\"oii_desc\">By ").append(storeExtension.getAuthors().get(0).getName()).append(", last updated ").append(WebUtils.elapsedSince(storeExtension.getUpdateDate())).append(" ago</div>")
                .append("</div>")

                .append("<div onclick=\"").append(id).append(".onClick()\" class=\"overview_item_msgs clickable\">")
                .append("<div class=\"oim_top\">").append("Version: ").append(displayVersion()).append("</div>")
                .append("<div class=\"oim_bottom\">").append("Rating: ").append(storeExtension.getRating()).append("</div>")
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
