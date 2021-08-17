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
}
