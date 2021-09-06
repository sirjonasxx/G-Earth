package gearth.extensions.parsers.catalog;

import gearth.extensions.parsers.HProductType;
import gearth.protocol.HPacket;

public class HProduct {
    private HProductType productType;
    private int furniClassId = -1;
    private String extraParam;
    private int productCount = 1;
    private boolean uniqueLimitedItem = false;
    private int uniqueLimitedItemSeriesSize = -1;
    private int uniqueLimitedItemsLeft = -1;

    protected HProduct(HPacket packet) {
        this.productType = HProductType.fromString(packet.readString());
        if(this.productType != HProductType.Badge) {
            this.furniClassId = packet.readInteger();
            this.extraParam = packet.readString();
            this.productCount = packet.readInteger();
            this.uniqueLimitedItem = packet.readBoolean();
            if(this.uniqueLimitedItem) {
                this.uniqueLimitedItemSeriesSize = packet.readInteger();
                this.uniqueLimitedItemsLeft = packet.readInteger();
            }
        } else {
            this.extraParam = packet.readString();
        }
    }

    public HProductType getProductType() {
        return productType;
    }

    public int getFurniClassId() {
        return furniClassId;
    }

    public String getExtraParam() {
        return extraParam;
    }

    public int getProductCount() {
        return productCount;
    }

    public boolean isUniqueLimitedItem() {
        return uniqueLimitedItem;
    }

    public int getUniqueLimitedItemSeriesSize() {
        return uniqueLimitedItemSeriesSize;
    }

    public int getUniqueLimitedItemsLeft() {
        return uniqueLimitedItemsLeft;
    }
}
