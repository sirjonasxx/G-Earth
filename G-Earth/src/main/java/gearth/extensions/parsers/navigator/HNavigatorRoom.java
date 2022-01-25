package gearth.extensions.parsers.navigator;

import gearth.protocol.HPacket;

import java.util.*;

public class HNavigatorRoom {
    private int flatId;
    private String roomName;
    private int ownerId;
    private String ownerName;
    private int doorMode;
    private int userCount;
    private int maxUserCount;
    private String description;
    private int tradeMode;
    private int score;
    private int ranking;
    private int categoryId;
    private String[] tags;

    private String officialRoomPicRef = null;

    private int groupId = -1;
    private String groupName = null;
    private String groupBadgeCode = null;

    private String roomAdName = null;
    private String roomAdDescription = null;
    private int roomAdExpiresInMin = -1;

    private boolean showOwner;
    private boolean allowPets;
    private boolean displayRoomEntryAd;

    public HNavigatorRoom(HPacket packet) {
        this.flatId = packet.readInteger();
        this.roomName = packet.readString();
        this.ownerId = packet.readInteger();
        this.ownerName = packet.readString();
        this.doorMode = packet.readInteger();
        this.userCount = packet.readInteger();
        this.maxUserCount = packet.readInteger();
        this.description = packet.readString();
        this.tradeMode = packet.readInteger();
        this.score = packet.readInteger();
        this.ranking = packet.readInteger();
        this.categoryId = packet.readInteger();

        this.tags = new String[packet.readInteger()];
        for (int i = 0; i < tags.length; i++) {
            this.tags[i] = packet.readString();
        }

        int multiUse = packet.readInteger();

        if ((multiUse & 1) > 0) {
            this.officialRoomPicRef = packet.readString();
        }

        if ((multiUse & 2) > 0) {
            this.groupId = packet.readInteger();
            this.groupName = packet.readString();
            this.groupBadgeCode = packet.readString();
        }

        if ((multiUse & 4) > 0) {
            this.roomAdName = packet.readString();
            this.roomAdDescription = packet.readString();
            this.roomAdExpiresInMin = packet.readInteger();
        }

        this.showOwner = (multiUse & 8) > 0;
        this.allowPets = (multiUse & 16) > 0;
        this.displayRoomEntryAd = (multiUse & 32) > 0;
    }

    public void appendToPacket(HPacket packet) {
        packet.appendInt(this.flatId);
        packet.appendString(this.roomName);
        packet.appendInt(this.ownerId);
        packet.appendString(this.ownerName);
        packet.appendInt(this.doorMode);
        packet.appendInt(this.userCount);
        packet.appendInt(this.maxUserCount);
        packet.appendString(this.description);
        packet.appendInt(this.tradeMode);
        packet.appendInt(this.score);
        packet.appendInt(this.ranking);
        packet.appendInt(this.categoryId);

        packet.appendInt(this.tags.length);
        for(String tag : tags) {
            packet.appendString(tag);
        }

        int multiUse = 0;
        List<Object> objectsToAppend = new ArrayList<>();
        if (this.officialRoomPicRef != null) {
            multiUse |= 1;
            objectsToAppend.add(this.officialRoomPicRef);
        }

        if (this.groupId != -1 && this.groupName != null && this.groupBadgeCode != null) {
            multiUse |= 2;
            objectsToAppend.add(this.groupId);
            objectsToAppend.add(this.groupName);
            objectsToAppend.add(this.groupBadgeCode);
        }

        if (this.roomAdName != null && this.roomAdDescription != null && this.roomAdExpiresInMin != -1) {
            multiUse |= 4;
            objectsToAppend.add(this.roomAdName);
            objectsToAppend.add(this.roomAdDescription);
            objectsToAppend.add(this.roomAdExpiresInMin);
        }

        if (this.showOwner) {
            multiUse |= 8;
        }

        if (this.allowPets) {
            multiUse |= 16;
        }

        if (this.displayRoomEntryAd) {
            multiUse |= 32;
        }

        packet.appendInt(multiUse);
        packet.appendObjects(objectsToAppend);
    }

    public int getFlatId() {
        return flatId;
    }

    public void setFlatId(int flatId) {
        this.flatId = flatId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public int getDoorMode() {
        return doorMode;
    }

    public void setDoorMode(int doorMode) {
        this.doorMode = doorMode;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public int getMaxUserCount() {
        return maxUserCount;
    }

    public void setMaxUserCount(int maxUserCount) {
        this.maxUserCount = maxUserCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTradeMode() {
        return tradeMode;
    }

    public void setTradeMode(int tradeMode) {
        this.tradeMode = tradeMode;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    
}
