package gearth.services.internal_extensions.extensionstore.repository.querying;

public enum ExtensionOrdering {

    RATING("Rating"),
    ALPHABETICAL("Alphabetical"),
    LAST_UPDATED("Last updated"),
    NEW_RELEASES("New releases");



    private String orderName;

    ExtensionOrdering(String orderName) {
        this.orderName = orderName;
    }

    public String getOrderName() {
        return orderName;
    }

    public static ExtensionOrdering fromString(String text) {
        for (ExtensionOrdering b : ExtensionOrdering.values()) {
            if (b.orderName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }
}
