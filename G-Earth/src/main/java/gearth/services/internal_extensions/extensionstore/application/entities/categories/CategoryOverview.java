package gearth.services.internal_extensions.extensionstore.application.entities.categories;

import gearth.GEarth;
import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.services.internal_extensions.extensionstore.repository.models.ExtCategory;
import gearth.ui.translations.LanguageBundle;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryOverview extends HOverview {

    protected final StoreRepository storeRepository;

    public CategoryOverview(HOverview parent, int startIndex, int size, StoreRepository storeRepository) {
        super(parent, startIndex, size);
        this.storeRepository = storeRepository;
    }


    @Override
    public String buttonText() {
        return null;
    }

    @Override
    public boolean buttonEnabled() {
        return false;
    }

    @Override
    public List<? extends ContentItem> getContentItems() {
        List<ExtCategory> categories = storeRepository.getCategories();

        return categories.subList(startIndex, Math.min(startIndex + limit, categories.size())).stream()
                .map(CategoryItem::new).collect(Collectors.toList());
    }

    @Override
    public int getMaxAmount() {
        return storeRepository.getCategories().size();
    }

    @Override
    public void buttonClick(GExtensionStore gExtensionStore) {
        // button is disabled
    }

    @Override
    public Header header() {
        return new Header() {
            @Override
            public String iconUrl() {
                return "images/overviews/idea.png";
            }

            @Override
            public String title() {
                return LanguageBundle.get("ext.store.categories.title");
            }

            @Override
            public String description() {
                return LanguageBundle.get("ext.store.categories.description");
            }

            @Override
            public String contentTitle() {
                return LanguageBundle.get("ext.store.categories.contenttitle");
            }
        };
    }

    @Override
    public HOverview getNewPage(int startIndex, int size) {
        return new CategoryOverview(parent, startIndex, size, storeRepository);
    }

}
