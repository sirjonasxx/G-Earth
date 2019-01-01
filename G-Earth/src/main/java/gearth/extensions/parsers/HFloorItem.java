package gearth.extensions.parsers;

import gearth.protocol.HPacket;

import java.util.HashMap;

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

    public HFloorItem(HPacket packet) {
        id = packet.readInteger();
        typeId = packet.readInteger();

        int x = packet.readInteger();
        int y = packet.readInteger();
        facing = HDirection.values()[packet.readInteger()];

        tile = new HPoint(x, y, Double.parseDouble(packet.readString()));

        packet.readString();
        packet.readInteger();

        category = packet.readInteger();

        setStuffData(packet);

        secondsToExpiration = packet.readInteger();
        usagePolicy = packet.readInteger();

        ownerId = packet.readInteger();

        if (typeId < 0)
            packet.readString();

    }

    private void setStuffData(HPacket packet)
    {
        int kind = packet.readInteger();
        switch(kind)
        {
            case 0: // RegularFurni
                packet.readString();
                break;
            case 1: // MapStuffData
            {
                int max = packet.readInteger();
                for (int i = 0; i < max; i++) {
                    packet.readString();
                    packet.readString();
                }
            }
            break;
            case 2: // StringArrayStuffData
            {
                int max = packet.readInteger();
                for (int i = 0; i < max; i++)
                    packet.readString();
            }
            break;
            case 3: // idk about this one lol
                packet.readString();
                packet.readInteger();
                break;
            case 4: // neither about this one
                break;
            case 5: // IntArrayStuffData
            {
                int max = packet.readInteger();

                for (int i = 0; i < max; i++)
                    packet.readInteger();
            }
            break;
            case 6: // HighScoreStuffData
            {
                packet.readString();
                packet.readInteger();
                packet.readInteger();
                int max = packet.readInteger();

                for (int i = 0; i < max; i++) {
                    packet.readInteger();
                    int dataCount = packet.readInteger();
                    for (int j = 0; j < dataCount; j++)
                        packet.readString();
                }
            }
            break;
            case 7: // Crackables (Eggs and stuff)
                packet.readString();
                packet.readInteger();
                packet.readInteger();
                break;
        }
    }

    public static HFloorItem[] parse(HPacket packet) {
        int ownersCount = packet.readInteger();
        HashMap<Integer, String> owners = new HashMap<>(ownersCount);

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
