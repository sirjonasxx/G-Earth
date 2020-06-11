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

    public void appendToPacket(HPacket packet) {
        //            id = packet.readInteger();
        packet.appendInt(id);

//            typeId = packet.readInteger();
        packet.appendInt(typeId);

//            int x = packet.readInteger();
        packet.appendInt(tile.getX());

//            int y = packet.readInteger();
        packet.appendInt(tile.getY());

//            facing = HDirection.values()[packet.readInteger()];
        packet.appendInt(facing.ordinal());

//            tile = new HPoint(x, y, Double.parseDouble(packet.readString()));
        packet.appendString(tile.getZ() + "");


//            ignore1 = packet.readString();
        packet.appendString(ignore1);

//            ignore2 = packet.readInteger();
        packet.appendInt(ignore2);

//            category = packet.readInteger();
        packet.appendInt(category);


//            stuff = HStuff.readData(packet, category);
        for (Object object : stuff) {
            packet.appendObject(object);
        }

//            secondsToExpiration = packet.readInteger();
        packet.appendInt(secondsToExpiration);

//            usagePolicy = packet.readInteger();
        packet.appendInt(usagePolicy);

//            ownerId = packet.readInteger();
        packet.appendInt(ownerId);


        if (typeId < 0) {
            // ignore3 = packet.readString();
            packet.appendString(ignore3);
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
            furni.setOwnerName(owners.get(furni.ownerId));

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
            floorItem.appendToPacket(packet);
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

    public Object[] getStuff() {
        return stuff;
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

    public void setTile(HPoint tile) {
        this.tile = tile;
    }

    public void setFacing(HDirection facing) {
        this.facing = facing;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public void setSecondsToExpiration(int secondsToExpiration) {
        this.secondsToExpiration = secondsToExpiration;
    }

    public void setUsagePolicy(int usagePolicy) {
        this.usagePolicy = usagePolicy;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public void setStuff(Object[] stuff) {
        this.stuff = stuff;
    }
}
