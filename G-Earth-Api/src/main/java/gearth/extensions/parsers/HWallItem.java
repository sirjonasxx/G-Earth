package gearth.extensions.parsers;

import gearth.protocol.HPacket;

import java.util.HashMap;
import java.util.Map;

public class HWallItem implements IFurni {
    private int id;
    private int typeId;

    private String state;
    private String location;
    private int usagePolicy;
    private int secondsToExpiration;

    private int ownerId;
    private String ownerName;

    public HWallItem(HPacket packet) {
        id = Integer.decode(packet.readString());
        typeId = packet.readInteger();

        location = packet.readString();
        state = packet.readString();
        secondsToExpiration = packet.readInteger();
        usagePolicy = packet.readInteger();

        ownerId = packet.readInteger();
    }

    public void appendToPacket(HPacket packet) {
//            id = Integer.decode(packet.readString());
        packet.appendString(id + "");

//            typeId = packet.readInteger();
        packet.appendInt(typeId);

//            location = packet.readString();
        packet.appendString(location);

//            state = packet.readString();
        packet.appendString(state);

//            secondsToExpiration = packet.readInteger();
        packet.appendInt(secondsToExpiration);

//            usagePolicy = packet.readInteger();
        packet.appendInt(usagePolicy);

//            ownerId = packet.readInteger();
        packet.appendInt(ownerId);
    }

    public static HWallItem[] parse(HPacket packet) {
        int ownersCount = packet.readInteger();
        HashMap<Integer, String> owners = new HashMap<>(ownersCount);

        for (int i = 0; i < ownersCount; i++)
            owners.put(packet.readInteger(), packet.readString());

        HWallItem[] furniture = new HWallItem[packet.readInteger()];

        for (int i = 0; i < furniture.length; i++) {
            HWallItem furni = new HWallItem(packet);
            furni.setOwnerName(owners.get(furni.ownerId));

            furniture[i] = furni;
        }
        return furniture;
    }

    public static HPacket constructPacket(HWallItem[] wallItems, int headerId) {
        Map<Integer, String> owners = new HashMap<>();
        for (HWallItem wallItem : wallItems) {
            owners.put(wallItem.ownerId, wallItem.getOwnerName());
        }

        HPacket packet = new HPacket(headerId);
        packet.appendInt(owners.size());
        for (Integer ownerId : owners.keySet()) {
            packet.appendInt(ownerId);
            packet.appendString(owners.get(ownerId));
        }

        packet.appendInt(wallItems.length);
        for (HWallItem wallItem : wallItems) {
            wallItem.appendToPacket(packet);
        }

        return packet;
    }

    public int getId() {
        return id;
    }

    public int getTypeId() {
        return typeId;
    }

    public int getUsagePolicy() {
        return usagePolicy;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getState() {
        return state;
    }

    public String getLocation() {
        return location;
    }

    public int getSecondsToExpiration() {
        return secondsToExpiration;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setUsagePolicy(int usagePolicy) {
        this.usagePolicy = usagePolicy;
    }

    public void setSecondsToExpiration(int secondsToExpiration) {
        this.secondsToExpiration = secondsToExpiration;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }
}
