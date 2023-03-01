package gearth.services.internal_extensions.extensionstore.application.entities.search;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.services.internal_extensions.extensionstore.application.entities.queriedoverviews.SearchedQueryOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.ui.translations.LanguageBundle;

import java.util.Collections;
import java.util.List;

public class SearchOverview extends HOverview {

    private static SearchComponent searchComponent = null;
    private final StoreRepository storeRepository;

    public SearchOverview(HOverview parent, StoreRepository storeRepository) {
        super(null, 0, 1);
        this.storeRepository = storeRepository;

        if (searchComponent == null || searchComponent.getRepository() != storeRepository) {
            searchComponent = new SearchComponent(storeRepository);
        }
    }


    @Override
    public String buttonText() {
        return LanguageBundle.get("ext.store.button.search");
    }

    @Override
    public boolean buttonEnabled() {
        return true;
    }

    @Override
    public List<? extends ContentItem> getContentItems() {
        return Collections.singletonList(searchComponent);
    }

    @Override
    public int getMaxAmount() {
        return 1;
    }

    @Override
    public void buttonClick(GExtensionStore gExtensionStore) {
        gExtensionStore.getController().pushOverview(
                new SearchedQueryOverview(
                        gExtensionStore.getController().getCurrentOverview(),
                        0,
                        GExtensionStore.MAX_PAGES,
                        storeRepository,
                        searchComponent.getSearchKeyword(),
                        searchComponent.getOrdering(),
                        searchComponent.getClients(),
                        searchComponent.getFrameworks(),
                        searchComponent.getCategories(),
                        false,
                        false
                )
        );
    }

    @Override
    public HOverview.Header header() {
        return new Header() {
            @Override
            public String iconUrl() {
                return "images/overviews/search.png";
            }

            @Override
            public String title() {
                return LanguageBundle.get("ext.store.search.title");
            }

            @Override
            public String description() {
                return LanguageBundle.get("ext.store.search.description");
            }

            @Override
            public String contentTitle() {
                return LanguageBundle.get("ext.store.search.contenttitle");
            }
        };
    }

    @Override
    public HOverview getNewPage(int startIndex, int size) {
        return null; // impossible
    }

}
