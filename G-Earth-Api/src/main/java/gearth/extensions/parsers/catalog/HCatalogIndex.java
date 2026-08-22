package gearth.extensions.parsers.catalog;

import gearth.protocol.HPacket;

public class HCatalogIndex {
    private final HCatalogPageIndex root;
    private final boolean newAdditionsAvailable;
    private final String catalogType;

    public HCatalogPageIndex getRoot() {
        return root;
    }

    public boolean isNewAdditionsAvailable() {
        return newAdditionsAvailable;
    }

    public String getCatalogType() {
        return catalogType;
    }

    public HCatalogIndex(HPacket packet) {
        this.root = new HCatalogPageIndex(packet);
        this.newAdditionsAvailable = packet.readBoolean();
        this.catalogType = packet.readString();
    }
}
