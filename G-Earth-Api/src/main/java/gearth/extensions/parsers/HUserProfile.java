package gearth.extensions.parsers;

import gearth.protocol.HPacket;

public class HUserProfile {
    private int id;
    private String username;
    private String motto;
    private String figure;
    private String creationDate;
    private int achievementScore;
    private int friendCount;

    private boolean isFriend;
    private boolean isRequestedFriend;
    private boolean isOnline;

    private HGroup[] groups;

    private int lastAccessSince;
    private boolean openProfile;

    public HUserProfile(HPacket packet) {
        id = packet.readInteger();
        username = packet.readString();
        motto = packet.readString();
        figure = packet.readString();
        creationDate = packet.readString();
        achievementScore = packet.readInteger();
        friendCount = packet.readInteger();

        isFriend = packet.readBoolean();
        isRequestedFriend = packet.readBoolean();
        isOnline = packet.readBoolean();

        groups = new HGroup[packet.readInteger()];
        for (int i = 0; i < groups.length; i++)
            groups[i] = new HGroup(packet);

        lastAccessSince = packet.readInteger();
        openProfile = packet.readBoolean();
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getMotto() {
        return motto;
    }

    public String getFigure() {
        return figure;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public int getAchievementScore() {
        return achievementScore;
    }

    public int getFriendCount() {
        return friendCount;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public boolean isRequestedFriend() {
        return isRequestedFriend;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public HGroup[] getGroups() {
        return groups;
    }

    public int getLastAccessSince() {
        return lastAccessSince;
    }

    public boolean isOpenProfile() {
        return openProfile;
    }
}
