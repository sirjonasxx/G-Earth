package gearth.extensions.parsers;

import gearth.protocol.HPacket;

public class HEntity {
    private int id;
    private int index;
    private HPoint tile;
    private String name;
    private String motto;
    private HGender gender = null;
    private HEntityType entityType;
    private String figureId;
    private String favoriteGroup = null;
    private HEntityUpdate lastUpdate = null;

    public HEntity(HPacket packet) {
        id = packet.readInteger();
        name = packet.readString();
        motto = packet.readString();
        figureId = packet.readString();
        index = packet.readInteger();
        tile = new HPoint(packet.readInteger(), packet.readInteger(),
                Double.parseDouble(packet.readString()));

        packet.readInteger();
        int entityTypeId = packet.readInteger();
        entityType = HEntityType.valueOf(entityTypeId);

        switch (entityTypeId) {
            case 1:
                gender = HGender.fromString(packet.readString());
                packet.readInteger();
                packet.readInteger();
                favoriteGroup = packet.readString();
                packet.readString();
                packet.readInteger();
                packet.readBoolean();
                break;
            case 2:
                packet.readInteger();
                packet.readInteger();
                packet.readString();
                packet.readInteger();
                packet.readBoolean();
                packet.readBoolean();
                packet.readBoolean();
                packet.readBoolean();
                packet.readBoolean();
                packet.readBoolean();
                packet.readInteger();
                packet.readString();
                break;
            case 4:

                packet.readString();
                packet.readInteger();
                packet.readString();
                for (int j = packet.readInteger(); j > 0; j--)
                {
                    packet.readShort();
                }
                break;
        }
    }

    public static HEntity[] parse(HPacket packet) {
        HEntity[] entities = new HEntity[packet.readInteger()];

        for (int i = 0; i < entities.length; i++)
            entities[i] = new HEntity(packet);

        return entities;
    }

    public boolean tryUpdate(HEntityUpdate update) {
        if (index != update.getIndex()) return false;

        tile = update.getTile();
        lastUpdate = update;
        return true;
    }

    public int getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    public HPoint getTile() {
        return tile;
    }

    public String getName() {
        return name;
    }

    public String getMotto() {
        return motto;
    }

    public HGender getGender() {
        return gender;
    }

    public HEntityType getEntityType() {
        return entityType;
    }

    public String getFigureId() {
        return figureId;
    }

    public String getFavoriteGroup() {
        return favoriteGroup;
    }

    public HEntityUpdate getLastUpdate() {
        return lastUpdate;
    }
}
