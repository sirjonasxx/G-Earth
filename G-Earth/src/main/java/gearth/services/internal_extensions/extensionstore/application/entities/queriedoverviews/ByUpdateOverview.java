package gearth.services.internal_extensions.extensionstore.application.entities.queriedoverviews;

import gearth.misc.OSValidator;
import gearth.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.services.internal_extensions.extensionstore.repository.querying.ExtensionOrdering;

import java.util.Collections;
import java.util.List;

public class ByUpdateOverview extends QueriedExtensionOverview {


    public ByUpdateOverview(HOverview parent, int startIndex, int size, StoreRepository storeRepository) {
        super(parent, startIndex, size, storeRepository);
    }

    protected List<StoreExtension> query(int startIndex, int size) {
        return storeRepository.getExtensions(startIndex, size, "", ExtensionOrdering.LAST_UPDATED,
                Collections.singletonList(OSValidator.getOSFull()), null, null, null, false, false);
    }

    @Override
    public Header header() {
        return new Header() {
            @Override
            public String iconUrl() {
                return "images/overviews/clock.png";
            }

            @Override
            public String title() {
                return "Recently Updated";
            }

            @Override
            public String description() {
                return "Extensions that were recently updated";
            }

            @Override
            public String contentTitle() {
                return "Recently Updated";
            }
        };
    }


    @Override
    public HOverview getNewPage(int startIndex, int size) {
        return new ByUpdateOverview(parent, startIndex, size, storeRepository);
    }
}
