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

    public static HWallItem[] parse(HPacket packet) {
        int ownersCount = packet.readInteger();
        HashMap<Integer, String> owners = new HashMap<>(ownersCount);

        for (int i = 0; i < ownersCount; i++)
            owners.put(packet.readInteger(), packet.readString());

        HWallItem[] furniture = new HWallItem[packet.readInteger()];

        for (int i = 0; i < furniture.length; i++) {
            HWallItem furni = new HWallItem(packet);
            furni.ownerName = owners.get(furni.ownerId);

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
//            id = Integer.decode(packet.readString());
            packet.appendString(wallItem.id + "");

//            typeId = packet.readInteger();
            packet.appendInt(wallItem.typeId);

//            location = packet.readString();
            packet.appendString(wallItem.location);

//            state = packet.readString();
            packet.appendString(wallItem.state);

//            secondsToExpiration = packet.readInteger();
            packet.appendInt(wallItem.secondsToExpiration);

//            usagePolicy = packet.readInteger();
            packet.appendInt(wallItem.usagePolicy);

//            ownerId = packet.readInteger();
            packet.appendInt(wallItem.ownerId);
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
}
