package gearth.extensions.parsers.catalog;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HCatalogPageIndex {
    private final boolean visible;
    private final int icon, pageId;
    private final String pageName, localization;
    private final List<Integer> offerIds = new ArrayList<>();
    private final List<HCatalogPageIndex> children = new ArrayList<>();

    public boolean isVisible() {
        return visible;
    }

    public int getIcon() {
        return icon;
    }

    public int getPageId() {
        return pageId;
    }

    public String getPageName() {
        return pageName;
    }

    public String getLocalization() {
        return localization;
    }

    public List<Integer> getOfferIds() {
        return Collections.unmodifiableList(offerIds);
    }

    public List<HCatalogPageIndex> getChildren() {
        return Collections.unmodifiableList(children);
    }

    protected HCatalogPageIndex(HPacket packet) {
        this.visible = packet.readBoolean();
        this.icon = packet.readInteger();
        this.pageId = packet.readInteger();
        this.pageName = packet.readString();
        this.localization = packet.readString();

        int offerCount = packet.readInteger();
        for(int i = 0; i < offerCount; i++) {
            this.offerIds.add(packet.readInteger());
        }

        int childCount = packet.readInteger();
        for(int i = 0; i < childCount; i++) {
            this.children.add(new HCatalogPageIndex(packet));
        }
    }
}
