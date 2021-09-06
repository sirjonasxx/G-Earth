package gearth.extensions.parsers;

import gearth.protocol.HPacket;

public class HInventoryItem {
    private int placementId;
    private HProductType type;
    private int id;
    private int typeId;
    private HSpecialType specialType;
    private int category;
    private Object[] stuff;
    private boolean recyclable;
    private boolean tradeable;
    private boolean groupable;
    private boolean sellable;
    private int secondsToExpiration;
    private boolean rentPeriodStarted;
    private int roomId;

    private String slotId = "";
    private int extra = -1;

    public HInventoryItem(HPacket packet) {
        placementId = packet.readInteger();
        type = HProductType.fromString(packet.readString());
        id = packet.readInteger();
        typeId = packet.readInteger();
        specialType = HSpecialType.fromId(packet.readInteger());
        category = packet.readInteger();
        stuff = HStuff.readData(packet, category);
        recyclable = packet.readBoolean();
        tradeable = packet.readBoolean();
        groupable = packet.readBoolean();
        sellable = packet.readBoolean();
        secondsToExpiration = packet.readInteger();
        rentPeriodStarted = packet.readBoolean();
        roomId = packet.readInteger();

        if(type == HProductType.FloorItem) {
            slotId = packet.readString();
            extra = packet.readInteger();
        }
    }

    public void appendToPacket(HPacket packet) {
        packet.appendInt(placementId);
        packet.appendString(type.toString());
        packet.appendInt(id);
        packet.appendInt(typeId);
        packet.appendInt(specialType.getId());
        packet.appendInt(category);

        for (Object object : stuff) {
            packet.appendObject(object);
        }

        packet.appendBoolean(recyclable);
        packet.appendBoolean(tradeable);
        packet.appendBoolean(groupable);
        packet.appendBoolean(sellable);
        packet.appendInt(secondsToExpiration);
        packet.appendBoolean(rentPeriodStarted);
        packet.appendInt(roomId);

        if(type == HProductType.FloorItem) {
            packet.appendString(slotId);
            packet.appendInt(extra);
        }
    }

    public static HInventoryItem[] parse(HPacket packet) {
        // int packetcount
        // int packetindex
        packet.setReadIndex(14);
        int itemCount = packet.readInteger();
        HInventoryItem[] items = new HInventoryItem[itemCount];
        for(int i = 0; i < itemCount; i++) {
            items[i] = new HInventoryItem(packet);
        }

        return items;
    }

    public static HPacket[] constructPackets(HInventoryItem[] items, int headerId) {
        int packetCount = (int) Math.ceil((double) items.length / 600);

        HPacket[] packets = new HPacket[packetCount];

        for(int i = 0; i < packetCount; i++) {
            packets[i] = new HPacket(headerId);
            packets[i].appendInt(packetCount);
            packets[i].appendInt(i);
            packets[i].appendInt(i == packetCount - 1 ? items.length % 600 : 600);

            for(int j = i * 600; j < items.length && j < (j + 1) * 600; j++) {
                items[j].appendToPacket(packets[i]);
            }
        }

        return packets;
    }

    public int getPlacementId() {
        return placementId;
    }

    public void setPlacementId(int placementId) {
        this.placementId = placementId;
    }

    public HProductType getType() {
        return type;
    }

    public void setType(HProductType type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public Object[] getStuff() {
        return stuff;
    }

    public void setStuff(Object[] stuff) {
        this.stuff = stuff;
    }

    public boolean isRecyclable() {
        return recyclable;
    }

    public void setRecyclable(boolean recyclable) {
        this.recyclable = recyclable;
    }

    public boolean isTradeable() {
        return tradeable;
    }

    public void setTradeable(boolean tradeable) {
        this.tradeable = tradeable;
    }

    public boolean isGroupable() {
        return groupable;
    }

    public void setGroupable(boolean groupable) {
        this.groupable = groupable;
    }

    public boolean isSellable() {
        return sellable;
    }

    public void setSellable(boolean sellable) {
        this.sellable = sellable;
    }

    public int getSecondsToExpiration() {
        return secondsToExpiration;
    }

    public void setSecondsToExpiration(int secondsToExpiration) {
        this.secondsToExpiration = secondsToExpiration;
    }

    public boolean isRentPeriodStarted() {
        return rentPeriodStarted;
    }

    public void setRentPeriodStarted(boolean rentPeriodStarted) {
        this.rentPeriodStarted = rentPeriodStarted;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public int getExtra() {
        return extra;
    }

    public void setExtra(int extra) {
        this.extra = extra;
    }
}
