package gearth.services.internal_extensions.extensionstore.application.entities;

import gearth.services.internal_extensions.extensionstore.GExtensionStore;

import java.util.List;

public abstract class HOverview {

    protected final HOverview parent;
    protected final int startIndex;
    protected final int limit;

    public HOverview(HOverview parent, int startIndex, int limit) {
        this.parent = parent;
        this.startIndex = startIndex;
        this.limit = limit;
    }

    public interface Header {

        String iconUrl();
        String title();
        String description();

        String contentTitle();

    }

    public abstract String buttonText();
    public abstract boolean buttonEnabled();

    public int getStartIndex() {
        return startIndex;
    }

    public int getAmount() { return getContentItems().size(); };
    public abstract List<? extends ContentItem> getContentItems();

    public abstract int getMaxAmount();
    public abstract void buttonClick(GExtensionStore gExtensionStore);

    // may be inherited from parent
    public abstract Header header();
    public abstract HOverview getNewPage(int startIndex, int size);
}
