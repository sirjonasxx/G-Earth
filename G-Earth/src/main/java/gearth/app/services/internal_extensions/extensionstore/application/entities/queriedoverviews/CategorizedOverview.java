package gearth.app.services.internal_extensions.extensionstore.application.entities.queriedoverviews;

import gearth.app.misc.OSValidator;
import gearth.app.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.app.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.app.services.internal_extensions.extensionstore.repository.models.ExtCategory;
import gearth.app.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.app.services.internal_extensions.extensionstore.repository.querying.ExtensionOrdering;
import gearth.app.ui.translations.LanguageBundle;

import java.util.Collections;
import java.util.List;

public class CategorizedOverview extends QueriedExtensionOverview {

    private final ExtCategory category;

    public CategorizedOverview(HOverview parent, int startIndex, int size, StoreRepository storeRepository, ExtCategory category) {
        super(parent, startIndex, size, storeRepository);
        this.category = category;
    }

    @Override
    protected List<StoreExtension> query(int startIndex, int size) {
        return storeRepository.getExtensions(startIndex, size, "", ExtensionOrdering.RATING,
                Collections.singletonList(OSValidator.getOSFull()), null, null,
                Collections.singletonList(category.getName()), false, false);
    }

    @Override
    public Header header() {
        return new Header() {
            @Override
            public String iconUrl() {
                return storeRepository.getResourceUrl("assets", "icons", category.getIcon());
            }

            @Override
            public String title() {
                return category.getName();
            }

            @Override
            public String description() {
                return category.getDescription();
            }

            @Override
            public String contentTitle() {
                return LanguageBundle.get("ext.store.category") + ": " + category.getName();
            }
        };
    }

    @Override
    public HOverview getNewPage(int startIndex, int size) {
        return new CategorizedOverview(parent, startIndex, size, storeRepository, category);
    }
}
