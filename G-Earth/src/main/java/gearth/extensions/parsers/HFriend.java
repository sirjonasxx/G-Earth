package gearth.extensions.parsers;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HFriend {
    private int id;
    private String name;
    private HGender gender;
    private boolean online;
    private boolean followingAllowed;
    private String figure;
    private int categoryId;
    private String motto;
    private String realName;
    private String facebookId;
    private boolean persistedMessageUser;
    private boolean vipMember;
    private boolean pocketHabboUser;
    private HRelationshipStatus relationshipStatus;

    private String categoryName = null;

    public HFriend(HPacket packet) {
        this.id = packet.readInteger();
        this.name = packet.readString();
        this.gender = packet.readInteger() == 0 ? HGender.Female : HGender.Male;
        this.online = packet.readBoolean();
        this.followingAllowed = packet.readBoolean();
        this.figure = packet.readString();
        this.categoryId = packet.readInteger();
        this.motto = packet.readString();
        this.realName = packet.readString();
        this.facebookId = packet.readString();
        this.persistedMessageUser = packet.readBoolean();
        this.vipMember = packet.readBoolean();
        this.pocketHabboUser = packet.readBoolean();
        this.relationshipStatus = HRelationshipStatus.fromId(packet.readShort());
    }

    public void appendToPacket(HPacket packet) {
        packet.appendInt(id);
        packet.appendString(name);
        packet.appendInt(gender == HGender.Female ? 0 : 1);
        packet.appendBoolean(online);
        packet.appendBoolean(followingAllowed);
        packet.appendString(figure);
        packet.appendInt(categoryId);
        packet.appendString(motto);
        packet.appendString(realName);
        packet.appendString(facebookId);
        packet.appendBoolean(persistedMessageUser);
        packet.appendBoolean(vipMember);
        packet.appendBoolean(pocketHabboUser);
        packet.appendShort((short) relationshipStatus.getId());
    }

    public static HFriend[] parseFromFragment(HPacket packet) {
        packet.setReadIndex(14);
        // int packetCount
        // int packetIndex
        HFriend[] friends = new HFriend[packet.readInteger()];

        for(int i = 0; i < friends.length; i++) {
            friends[i] = new HFriend(packet);
        }

        return friends;
    }

    public static HFriend[] parseFromUpdate(HPacket packet) {
        packet.resetReadIndex();
        int categoryCount = packet.readInteger();
        Map<Integer, String> categories = new HashMap<>();

        for(int i = 0; i < categoryCount; i++)
            categories.put(packet.readInteger(), packet.readString());

        int friendCount = packet.readInteger();
        List<HFriend> friends = new ArrayList<>();
        for(int i = 0; i < friendCount; i++) {
            if(packet.readInteger() != -1) {
                friends.add(new HFriend(packet));
            } else {
                packet.readInteger();
            }
        }

        for(HFriend friend : friends) {
            friend.categoryName = categories.getOrDefault(friend.categoryId, null);
        }

        return friends.toArray(new HFriend[0]);
    }

    public static int[] getRemovedFriendIdsFromUpdate(HPacket packet) {
        packet.resetReadIndex();
        int categoryCount = packet.readInteger();
        for(int i = 0; i < categoryCount; i++) {
            packet.readInteger();
            packet.readString();
        }

        int friendCount = packet.getReadIndex();
        List<Integer> removedIds = new ArrayList<>();
        for(int i = 0; i < friendCount; i++) {
            if(packet.readInteger() != -1) {
                new HFriend(packet);
            } else {
                removedIds.add(packet.readInteger());
            }
        }

        return removedIds.stream().mapToInt(i -> i).toArray();
    }

    public static HPacket[] constructFragmentPackets(HFriend[] friends, int headerId) {
        int packetCount = (int) Math.ceil((double) friends.length / 100);

        HPacket[] packets = new HPacket[packetCount];

        for(int i = 0; i < packetCount; i++) {
            packets[i] = new HPacket(headerId);
            packets[i].appendInt(packetCount);
            packets[i].appendInt(i);
            packets[i].appendInt(i == packetCount - 1 ? friends.length % 100 : 100);

            for(int j = i * 100; j < friends.length && j < (j + 1) * 100; j++) {
                friends[j].appendToPacket(packets[i]);
            }
        }

        return packets;
    }

    public static HPacket constructUpdatePacket(HFriend[] friends, int headerId) {
        Map<Integer, String> categories = new HashMap<>();
        for (HFriend friend : friends) {
            if(friend.categoryName != null)
                categories.put(friend.categoryId, friend.categoryName);
        }

        HPacket packet = new HPacket(headerId);
        packet.appendInt(categories.size());
        for(int categoryId : categories.keySet()) {
            packet.appendInt(categoryId);
            packet.appendString(categories.get(categoryId));
        }

        packet.appendInt(friends.length);
        for(HFriend friend : friends) {
            friend.appendToPacket(packet);
        }

        return packet;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HGender getGender() {
        return gender;
    }

    public void setGender(HGender gender) {
        this.gender = gender;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isFollowingAllowed() {
        return followingAllowed;
    }

    public void setFollowingAllowed(boolean followingAllowed) {
        this.followingAllowed = followingAllowed;
    }

    public String getFigure() {
        return figure;
    }

    public void setFigure(String figure) {
        this.figure = figure;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getMotto() {
        return motto;
    }

    public void setMotto(String motto) {
        this.motto = motto;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getFacebookId() {
        return facebookId;
    }

    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    public boolean isPersistedMessageUser() {
        return persistedMessageUser;
    }

    public void setPersistedMessageUser(boolean persistedMessageUser) {
        this.persistedMessageUser = persistedMessageUser;
    }

    public boolean isVipMember() {
        return vipMember;
    }

    public void setVipMember(boolean vipMember) {
        this.vipMember = vipMember;
    }

    public boolean isPocketHabboUser() {
        return pocketHabboUser;
    }

    public void setPocketHabboUser(boolean pocketHabboUser) {
        this.pocketHabboUser = pocketHabboUser;
    }

    public HRelationshipStatus getRelationshipStatus() {
        return relationshipStatus;
    }

    public void setRelationshipStatus(HRelationshipStatus relationshipStatus) {
        this.relationshipStatus = relationshipStatus;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
