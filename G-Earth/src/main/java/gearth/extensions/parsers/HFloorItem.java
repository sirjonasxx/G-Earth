package gearth.extensions.parsers;

import gearth.protocol.HPacket;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HFloorItem implements IFurni {
    private int id;
    private int typeId;
    private HPoint tile;
    private HDirection facing;

    private int category;

    private int secondsToExpiration;
    private int usagePolicy;
    private int ownerId;
    private String ownerName;
    private Object[] stuff;

    private String ignore1;
    private Integer ignore2;
    private String ignore3;

    public HFloorItem(HPacket packet) {
        id = packet.readInteger();
        typeId = packet.readInteger();

        int x = packet.readInteger();
        int y = packet.readInteger();
        facing = HDirection.values()[packet.readInteger()];

        tile = new HPoint(x, y, Double.parseDouble(packet.readString()));

        ignore1 = packet.readString();
        ignore2 = packet.readInteger();

        category = packet.readInteger();

        stuff = HStuff.readData(packet, category);

        secondsToExpiration = packet.readInteger();
        usagePolicy = packet.readInteger();

        ownerId = packet.readInteger();

        if (typeId < 0) {
            ignore3 = packet.readString();
        }
        else {
            ignore3 = null;
        }


    }

    public static HFloorItem[] parse(HPacket packet) {
        int ownersCount = packet.readInteger();
        Map<Integer, String> owners = new HashMap<>(ownersCount);

        for (int i = 0; i < ownersCount; i++)
            owners.put(packet.readInteger(), packet.readString());

        HFloorItem[] furniture = new HFloorItem[packet.readInteger()];
        for (int i = 0; i < furniture.length; i++) {
            HFloorItem furni = new HFloorItem(packet);
            furni.ownerName = owners.get(furni.ownerId);

            furniture[i] = furni;
        }
        return furniture;
    }

    public static HPacket constructPacket(HFloorItem[] floorItems, int headerId) {
        Map<Integer, String> owners = new HashMap<>();
        for (HFloorItem floorItem : floorItems) {
            owners.put(floorItem.ownerId, floorItem.getOwnerName());
        }

        HPacket packet = new HPacket(headerId);
        packet.appendInt(owners.size());
        for (Integer ownerId : owners.keySet()) {
            packet.appendInt(ownerId);
            packet.appendString(owners.get(ownerId));
        }

        packet.appendInt(floorItems.length);
        for (HFloorItem floorItem : floorItems) {
//            id = packet.readInteger();
            packet.appendInt(floorItem.id);

//            typeId = packet.readInteger();
            packet.appendInt(floorItem.typeId);

//            int x = packet.readInteger();
            packet.appendInt(floorItem.tile.getX());

//            int y = packet.readInteger();
            packet.appendInt(floorItem.tile.getY());

//            facing = HDirection.values()[packet.readInteger()];
            packet.appendInt(floorItem.facing.ordinal());

//            tile = new HPoint(x, y, Double.parseDouble(packet.readString()));
            packet.appendString(floorItem.tile.getZ() + "");


//            ignore1 = packet.readString();
            packet.appendString(floorItem.ignore1);

//            ignore2 = packet.readInteger();
            packet.appendInt(floorItem.ignore2);

//            category = packet.readInteger();
            packet.appendInt(floorItem.category);


//            stuff = HStuff.readData(packet, category);
            for (Object object : floorItem.stuff) {
                packet.appendObject(object);
            }

//            secondsToExpiration = packet.readInteger();
            packet.appendInt(floorItem.secondsToExpiration);

//            usagePolicy = packet.readInteger();
            packet.appendInt(floorItem.usagePolicy);

//            ownerId = packet.readInteger();
            packet.appendInt(floorItem.ownerId);


            if (floorItem.typeId < 0) {
                // ignore3 = packet.readString();
                packet.appendString(floorItem.ignore3);
            }
        }

        return packet;
    }


    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getTypeId() {
        return typeId;
    }

    @Override
    public int getUsagePolicy() {
        return usagePolicy;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getSecondsToExpiration() {
        return secondsToExpiration;
    }

    public int getCategory() {
        return category;
    }

    public HDirection getFacing() {
        return facing;
    }

    public HPoint getTile() {
        return tile;
    }
}
