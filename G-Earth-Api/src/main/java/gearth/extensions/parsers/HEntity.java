package gearth.extensions.parsers;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

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
    private Object[] stuff = new Object[0];

    private int unknown1;

    public HEntity(HPacket packet) {
        id = packet.readInteger();
        name = packet.readString();
        motto = packet.readString();
        figureId = packet.readString();
        index = packet.readInteger();
        tile = new HPoint(packet.readInteger(), packet.readInteger(),
                Double.parseDouble(packet.readString()));

        unknown1 = packet.readInteger();
        int entityTypeId = packet.readInteger();
        entityType = HEntityType.valueOf(entityTypeId);

        switch (entityTypeId) {
            case 1:
                stuff = new Object[5];
                gender = HGender.fromString(packet.readString());
                stuff[0] = packet.readInteger();
                stuff[1] = packet.readInteger();
                favoriteGroup = packet.readString();
                stuff[2] = packet.readString();
                stuff[3] = packet.readInteger();
                stuff[4] = packet.readBoolean();
                break;
            case 2:
                stuff = new Object[12];
                stuff[0] = packet.readInteger();
                stuff[1] = packet.readInteger();
                stuff[2] = packet.readString();
                stuff[3] = packet.readInteger();
                stuff[4] = packet.readBoolean();
                stuff[5] = packet.readBoolean();
                stuff[6] = packet.readBoolean();
                stuff[7] = packet.readBoolean();
                stuff[8] = packet.readBoolean();
                stuff[9] = packet.readBoolean();
                stuff[10] = packet.readInteger();
                stuff[11] = packet.readString();
                break;
            case 4:
                stuff = new Object[4];
                stuff[0] = packet.readString();
                stuff[1] = packet.readInteger();
                stuff[2] = packet.readString();
                List<Short> list = new ArrayList<>();
                for (int j = packet.readInteger(); j > 0; j--)
                {
                    list.add(packet.readShort());
                }
                stuff[3] = list;
                break;
        }
    }

    public void appendToPacket(HPacket packet) {
        packet.appendInt(id);
        packet.appendString(name);
        packet.appendString(motto);
        packet.appendString(figureId);
        packet.appendInt(index);
        packet.appendInt(tile.getX());
        packet.appendInt(tile.getY());
        packet.appendString(tile.getZ() + "");
        packet.appendInt(unknown1);
        packet.appendInt(entityType.getId());
//
        switch (entityType.getId()) {
            case 1:
                packet.appendObjects(gender.toString(), stuff[0], stuff[1],
                        favoriteGroup, stuff[2], stuff[3], stuff[4]);
                break;
            case 2:
                packet.appendObjects(stuff);
                break;
            case 4:
                packet.appendObjects(stuff[0], stuff[1], stuff[2]);
                List<Short> list = (List<Short>) stuff[3];
                packet.appendInt(list.size());
                for(short s : list) {
                    packet.appendShort(s);
                }

                break;
        }
    }

    public static HPacket constructPacket(HEntity[] entities, int headerId) {
        HPacket packet = new HPacket(headerId);
        packet.appendInt(entities.length);
        for (HEntity entity : entities) {
            entity.appendToPacket(packet);
        }

        return packet;
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

    public Object[] getStuff() {
        return stuff;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setTile(HPoint tile) {
        this.tile = tile;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMotto(String motto) {
        this.motto = motto;
    }

    public void setGender(HGender gender) {
        this.gender = gender;
    }

    public void setFigureId(String figureId) {
        this.figureId = figureId;
    }

    public void setFavoriteGroup(String favoriteGroup) {
        this.favoriteGroup = favoriteGroup;
    }

    public void setLastUpdate(HEntityUpdate lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public void setStuff(Object[] stuff) {
        this.stuff = stuff;
    }
}
