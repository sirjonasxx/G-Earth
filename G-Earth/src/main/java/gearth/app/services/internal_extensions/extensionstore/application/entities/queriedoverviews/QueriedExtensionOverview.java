package gearth.app.services.internal_extensions.extensionstore.application.entities.queriedoverviews;

import gearth.app.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.app.services.internal_extensions.extensionstore.application.entities.ContentItem;
import gearth.app.services.internal_extensions.extensionstore.application.entities.HOverview;
import gearth.app.services.internal_extensions.extensionstore.application.entities.StoreExtensionItem;
import gearth.app.services.internal_extensions.extensionstore.repository.StoreRepository;
import gearth.app.services.internal_extensions.extensionstore.repository.models.StoreExtension;

import java.util.List;
import java.util.stream.Collectors;

public abstract class QueriedExtensionOverview extends HOverview {

    protected final StoreRepository storeRepository;

    public QueriedExtensionOverview(HOverview parent, int startIndex, int size, StoreRepository storeRepository) {
        super(parent, startIndex, size);
        this.storeRepository = storeRepository;
    }

    protected abstract List<StoreExtension> query(int startIndex, int size);

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
        return query(startIndex, limit).stream().map(StoreExtensionItem::new).collect(Collectors.toList());
    }

    @Override
    public int getMaxAmount() {
        return query(0, -1).size();
    }

    @Override
    public void buttonClick(GExtensionStore gExtensionStore) {
        // nothing i think
    }

}
