package gearth.services.internal_extensions.extensionstore.application.entities.categories;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.services.internal_extensions.extensionstore.application.WebUtils;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.application.entities.queriedoverviews.CategorizedOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.ExtCategory;
import gearth.services.internal_extensions.extensionstore.repository.querying.ExtensionOrdering;
import gearth.ui.translations.LanguageBundle;
import netscape.javascript.JSObject;

import java.util.Collections;

public class CategoryItem implements ContentItem {

    private final ExtCategory category;
    private GExtensionStore gExtensionStore = null;

    public CategoryItem(ExtCategory category) {
        this.category = category;
    }

    public void onClick() {
        gExtensionStore.getController().pushOverview(
                new CategorizedOverview(
                        gExtensionStore.getController().getCurrentOverview(),
                        0,
                        GExtensionStore.MAX_PAGES,
                        gExtensionStore.getRepository(),
                        category
                )
        );
    }


    @Override
    public void addHtml(int i, GExtensionStore gExtensionStore) {
        this.gExtensionStore = gExtensionStore;
        StoreRepository repository = gExtensionStore.getRepository();

        String id = "category" + i + "_" + System.currentTimeMillis();

        int releasesCount = gExtensionStore.getRepository().getExtensions(0, -1, "", ExtensionOrdering.NEW_RELEASES,
                null, null, null, Collections.singletonList(category.getName()), false, false).size();

        StringBuilder htmlBuilder = new StringBuilder()
                .append("<div class=\"overview_item ").append(i % 2 == 0 ? "item_lightblue" : "item_darkblue").append(" content_item\">")

                .append("<div class=\"overview_item_logo\">")
                .append("<img src=\"").append(repository.getResourceUrl("assets", "icons", category.getIcon())).append("\" alt=\"\">")
                .append("</div>")

                .append("<div class=\"overview_item_info\">")
                .append("<div onclick=\"").append(id).append(".onClick()\" class=\"oii_name clickable\">").append(WebUtils.escapeMessage(category.getName())).append("</div>")
                .append("<div class=\"oii_desc\">").append(WebUtils.escapeMessage(category.getDescription())).append(" </div>")
                .append("</div>")

                .append("<div onclick=\"").append(id).append(".onClick()\" class=\"overview_item_msgs clickable\">")
                .append("<div class=\"oim_top\">").append(releasesCount).append(" ").append(LanguageBundle.get("ext.store.extension.author.releases")).append("</div>")
//                .append("<div class=\"oim_bottom\">").append(storeExtension.getFramework().getFramework().getName().replace("Native", "")).append(" </div>")
                .append("</div>")

                .append("</div>");

        String category = htmlBuilder.toString();
        GExtensionStoreController controller = gExtensionStore.getController();

        controller.getWebView().getEngine().executeScript("document.getElementById('" + controller.getContentItemsContainer() + "').innerHTML += '" + category + "';");

        JSObject window = (JSObject) controller.getWebView().getEngine().executeScript("window");
        window.setMember(id, this);
    }
}
