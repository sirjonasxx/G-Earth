package gearth.extensions.parsers.catalog;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HOffer {
    private int offerId;
    private String localizationId;
    private boolean rent;
    private int priceInCredits;
    private int priceInActivityPoints;
    private int activityPointType;
    private boolean giftable;
    private List<HProduct> products = new ArrayList<>();
    private int clubLevel;
    private boolean bundlePurchaseAllowed;
    private boolean isPet;
    private String previewImage;


    protected HOffer(HPacket packet) {
        this.offerId = packet.readInteger();
        this.localizationId = packet.readString();
        this.rent = packet.readBoolean();
        this.priceInCredits = packet.readInteger();
        this.priceInActivityPoints = packet.readInteger();
        this.activityPointType = packet.readInteger();
        this.giftable = packet.readBoolean();

        int productCount = packet.readInteger();
        for(int i = 0; i < productCount; i++) {
            this.products.add(new HProduct(packet));
        }

        this.clubLevel = packet.readInteger();
        this.bundlePurchaseAllowed = packet.readBoolean();
        this.isPet = packet.readBoolean();
        this.previewImage = packet.readString();
    }

    public int getOfferId() {
        return offerId;
    }

    public String getLocalizationId() {
        return localizationId;
    }

    public boolean isRent() {
        return rent;
    }

    public int getPriceInCredits() {
        return priceInCredits;
    }

    public int getPriceInActivityPoints() {
        return priceInActivityPoints;
    }

    public int getActivityPointType() {
        return activityPointType;
    }

    public boolean isGiftable() {
        return giftable;
    }

    public List<HProduct> getProducts() {
        return Collections.unmodifiableList(products);
    }

    public int getClubLevel() {
        return clubLevel;
    }

    public boolean isBundlePurchaseAllowed() {
        return bundlePurchaseAllowed;
    }

    public boolean isPet() {
        return isPet;
    }

    public String getPreviewImage() {
        return previewImage;
    }
}
