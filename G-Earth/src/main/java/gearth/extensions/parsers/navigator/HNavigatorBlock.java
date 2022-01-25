package gearth.extensions.parsers.navigator;

import gearth.protocol.HPacket;

import java.util.*;

public class HNavigatorBlock {
    private String searchCode;
    private String text;
    private int actionAllowed;
    private boolean isForceClosed;
    private int viewMode;
    private final List<HNavigatorRoom> rooms = new ArrayList<>();

    public HNavigatorBlock(HPacket packet) {
        this.searchCode = packet.readString();
        this.text = packet.readString();
        this.actionAllowed = packet.readInteger();
        this.isForceClosed = packet.readBoolean();
        this.viewMode = packet.readInteger();

        int count = packet.readInteger();
        for(int i = 0; i < count; i++) {
            rooms.add(new HNavigatorRoom(packet));
        }
    }

    public void appendToPacket(HPacket packet) {
        packet.appendString(this.searchCode);
        packet.appendString(this.text);
        packet.appendInt(this.actionAllowed);
        packet.appendBoolean(this.isForceClosed);
        packet.appendInt(this.viewMode);

        packet.appendInt(rooms.size());
        synchronized (rooms) {
            for(HNavigatorRoom room : rooms) {
                room.appendToPacket(packet);
            }
        }
    }

    public String getSearchCode() {
        return searchCode;
    }

    public void setSearchCode(String searchCode) {
        this.searchCode = searchCode;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getActionAllowed() {
        return actionAllowed;
    }

    public void setActionAllowed(int actionAllowed) {
        this.actionAllowed = actionAllowed;
    }

    public boolean isForceClosed() {
        return isForceClosed;
    }

    public void setForceClosed(boolean forceClosed) {
        isForceClosed = forceClosed;
    }

    public int getViewMode() {
        return viewMode;
    }

    public void setViewMode(int viewMode) {
        this.viewMode = viewMode;
    }

    public List<HNavigatorRoom> getRooms() {
        synchronized (rooms) {
            return Collections.unmodifiableList(rooms);
        }
    }

    public void removeRooms(HNavigatorRoom... removingRooms) {
        synchronized (rooms) {
            rooms.removeAll(Arrays.asList(removingRooms));
        }
    }

    public HNavigatorRoom removerRoom(int index) {
        synchronized (rooms) {
            return rooms.remove(index);
        }
    }

    public void addRooms(HNavigatorRoom... addingRooms) {
        synchronized (rooms) {
            rooms.addAll(Arrays.asList(addingRooms));
        }
    }
}
