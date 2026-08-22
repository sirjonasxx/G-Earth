package gearth.app.services.internal_extensions.extensionstore.application.entities.queriedoverviews;

import gearth.app.misc.OSValidator;
import gearth.app.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.app.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.app.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.app.services.internal_extensions.extensionstore.repository.querying.ExtensionOrdering;
import gearth.app.ui.translations.LanguageBundle;

import java.util.Collections;
import java.util.List;

public class ByRatingOverview extends QueriedExtensionOverview {

    public ByRatingOverview(HOverview parent, int startIndex, int size, StoreRepository storeRepository) {
        super(parent, startIndex, size, storeRepository);
    }

    protected List<StoreExtension> query(int startIndex, int size) {
        return storeRepository.getExtensions(startIndex, size, "", ExtensionOrdering.RATING,
                Collections.singletonList(OSValidator.getOSFull()), null, null, null, false, false);
    }


    @Override
    public Header header() {
        return new Header() {
            @Override
            public String iconUrl() {
                return "images/overviews/star.png";
            }

            @Override
            public String title() {
                return LanguageBundle.get("ext.store.search.ordering.byrating.title");
            }

            @Override
            public String description() {
                return LanguageBundle.get("ext.store.search.ordering.byrating.description");
            }

            @Override
            public String contentTitle() {
                return LanguageBundle.get("ext.store.search.ordering.byrating.contenttitle");
            }
        };
    }

    @Override
    public HOverview getNewPage(int startIndex, int size) {
        return new ByRatingOverview(parent, startIndex, size, storeRepository);
    }
}