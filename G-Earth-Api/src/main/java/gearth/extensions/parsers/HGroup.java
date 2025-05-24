package gearth.extensions.parsers;

import gearth.protocol.HPacket;

public class HGroup {
    private int id;
    private String name;
    private String badgeCode;
    private String primaryColor;
    private String secondaryColor;

    private boolean isFavorite;
    private int ownerId;
    private boolean hasForum;

    public HGroup(HPacket packet) {
        id = packet.readInteger();
        name = packet.readString();
        badgeCode = packet.readString();
        primaryColor = packet.readString();
        secondaryColor = packet.readString();

        isFavorite = packet.readBoolean();
        ownerId = packet.readInteger();
        hasForum = packet.readBoolean();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBadgeCode() {
        return badgeCode;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public boolean isHasForum() {
        return hasForum;
    }
}
