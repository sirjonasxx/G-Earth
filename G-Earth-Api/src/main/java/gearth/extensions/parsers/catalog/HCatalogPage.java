package gearth.extensions.parsers.catalog;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HCatalogPage {
    private int pageId;
    private String catalogType, layoutCode;
    private String[] images, texts;
    private List<HOffer> offers = new ArrayList<>();
    private int offerId;
    private boolean acceptSeasonCurrencyAsCredits;
    private List<HFrontPageItem> frontPageItems = new ArrayList<>();

    public HCatalogPage(HPacket packet) {
        this.pageId = packet.readInteger();
        this.catalogType = packet.readString();
        this.layoutCode = packet.readString();

        this.images = new String[packet.readInteger()];
        for(int i = 0; i < this.images.length; i++) {
            this.images[i] = packet.readString();
        }

        this.texts = new String[packet.readInteger()];
        for(int i = 0; i < this.texts.length; i++) {
            this.texts[i] = packet.readString();
        }

        int offerCount = packet.readInteger();
        for(int i = 0; i < offerCount; i++) {
            this.offers.add(new HOffer(packet));
        }

        this.offerId = packet.readInteger();
        this.acceptSeasonCurrencyAsCredits = packet.readBoolean();

        if(packet.getReadIndex() < packet.getBytesLength()) {
            int frontPageItemsCount = packet.readInteger();
            for(int i = 0; i < frontPageItemsCount; i++) {
                this.frontPageItems.add(new HFrontPageItem(packet));
            }
        }
    }

    public int getPageId() {
        return pageId;
    }

    public String getCatalogType() {
        return catalogType;
    }

    public String getLayoutCode() {
        return layoutCode;
    }

    public String[] getImages() {
        return images;
    }

    public String[] getTexts() {
        return texts;
    }

    public List<HOffer> getOffers() {
        return Collections.unmodifiableList(offers);
    }

    public int getOfferId() {
        return offerId;
    }

    public boolean isAcceptSeasonCurrencyAsCredits() {
        return acceptSeasonCurrencyAsCredits;
    }

    public List<HFrontPageItem> getFrontPageItems() {
        return Collections.unmodifiableList(frontPageItems);
    }
}
