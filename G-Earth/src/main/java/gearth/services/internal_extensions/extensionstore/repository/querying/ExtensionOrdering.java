package gearth.services.internal_extensions.extensionstore.repository.querying;

import gearth.ui.translations.LanguageBundle;

public enum ExtensionOrdering {

    RATING("ext.store.ordering.rating"),
    ALPHABETICAL("ext.store.ordering.alphabetical"),
    LAST_UPDATED("ext.store.ordering.lastupdated"),
    NEW_RELEASES("ext.store.ordering.newreleases");



    private String orderKey;

    ExtensionOrdering(String orderKey) {
        this.orderKey = orderKey;
    }

    public String getOrderName() {
        return LanguageBundle.get(orderKey);
    }

    public static ExtensionOrdering fromString(String text) {
        for (ExtensionOrdering b : ExtensionOrdering.values()) {
            if (LanguageBundle.get(b.orderKey).equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}
