package gearth.extensions.parsers;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HCatalogIndex {
    private final HPageIndex root;
    private final boolean newAdditionsAvailable;
    private final String catalogType;

    public HPageIndex getRoot() {
        return root;
    }

    public boolean isNewAdditionsAvailable() {
        return newAdditionsAvailable;
    }

    public String getCatalogType() {
        return catalogType;
    }

    public HCatalogIndex(HPacket packet) {
        this.root = new HPageIndex(packet);
        this.newAdditionsAvailable = packet.readBoolean();
        this.catalogType = packet.readString();
    }

    public static class HPageIndex {
        private final boolean visible;
        private final int icon, pageId;
        private final String pageName, localization;
        private final List<Integer> offerIds = new ArrayList<>();
        private final List<HPageIndex> children = new ArrayList<>();

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

        public List<HPageIndex> getChildren() {
            return Collections.unmodifiableList(children);
        }

        private HPageIndex(HPacket packet) {
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
                this.children.add(new HPageIndex(packet));
            }
        }
    }
}
