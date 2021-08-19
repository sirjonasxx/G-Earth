package gearth.services.internal_extensions.extensionstore.application.entities.queriedoverviews;

import gearth.misc.OSValidator;
import gearth.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.StoreExtension;
import gearth.services.internal_extensions.extensionstore.repository.querying.ExtensionOrdering;

import java.util.Collections;
import java.util.List;

public class ByDateOverview extends QueriedExtensionOverview {


    public ByDateOverview(HOverview parent, int startIndex, int size, StoreRepository storeRepository) {
        super(parent, startIndex, size, storeRepository);
    }

    protected List<StoreExtension> query(int startIndex, int size) {
        return storeRepository.getExtensions(startIndex, size, "", ExtensionOrdering.NEW_RELEASES,
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
                return "New Releases";
            }

            @Override
            public String description() {
                return "Extensions that were recently added to the G-ExtensionStore";
            }

            @Override
            public String contentTitle() {
                return "New Releases";
            }
        };
    }


    @Override
    public HOverview getNewPage(int startIndex, int size) {
        return new ByDateOverview(parent, startIndex, size, storeRepository);
    }
}
