package gearth.extensions.parsers.catalog;

import gearth.protocol.HPacket;

public class HFrontPageItem {
    private int position;
    private String itemName;
    private String itemPromoImage;
    private int type;
    private String cataloguePageLocation;
    private int productOfferId = -1;
    private String productCode;
    private int expirationTime;

    protected HFrontPageItem(HPacket packet) {
        this.position = packet.readInteger();
        this.itemName = packet.readString();
        this.itemPromoImage = packet.readString();
        this.type = packet.readInteger();

        switch(type) {
            case 0:
                this.cataloguePageLocation = packet.readString();
                break;
            case 1:
                this.productOfferId = packet.readInteger();
                break;
            case 2:
                this.productCode = packet.readString();
                break;
        }

        this.expirationTime = packet.readInteger();
    }

    public int getPosition() {
        return position;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemPromoImage() {
        return itemPromoImage;
    }

    public int getType() {
        return type;
    }

    public String getCataloguePageLocation() {
        return cataloguePageLocation;
    }

    public int getProductOfferId() {
        return productOfferId;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getExpirationTime() {
        return expirationTime;
    }
}
