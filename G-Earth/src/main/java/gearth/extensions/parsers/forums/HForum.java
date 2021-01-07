package gearth.extensions.parsers.forums;

import gearth.protocol.HPacket;

public class HForum {

    private final int guildId;
    private final String guildName;
    private final String guildDescription;
    private final String guildBadge;

    private final int unknown1;
    private final int rating;
    private final int amountComments;
    private final int unreadComments;

    private final int lastCommentIndexInForum;
    private final int lastCommentUserId;
    private final String lastCommentUserName;
    private final int lastCommentPassedTime;


    public HForum(HPacket hPacket) {
        guildId = hPacket.readInteger();
        guildName = hPacket.readString();
        guildDescription = hPacket.readString();
        guildBadge = hPacket.readString();

        unknown1 = hPacket.readInteger();
        rating = hPacket.readInteger();
        amountComments = hPacket.readInteger();
        unreadComments = hPacket.readInteger();

        lastCommentIndexInForum = hPacket.readInteger();
        lastCommentUserId = hPacket.readInteger();
        lastCommentUserName = hPacket.readString();
        lastCommentPassedTime = hPacket.readInteger();
    }

    public int getGuildId() {
        return guildId;
    }

    public String getGuildName() {
        return guildName;
    }

    public String getGuildDescription() {
        return guildDescription;
    }

    public String getGuildBadge() {
        return guildBadge;
    }

    public int getUnknown1() {
        return unknown1;
    }

    public int getRating() {
        return rating;
    }

    public int getAmountComments() {
        return amountComments;
    }

    public int getUnreadComments() {
        return unreadComments;
    }

    public int getLastCommentIndexInForum() {
        return lastCommentIndexInForum;
    }

    public int getLastCommentUserId() {
        return lastCommentUserId;
    }

    public String getLastCommentUserName() {
        return lastCommentUserName;
    }

    public int getLastCommentPassedTime() {
        return lastCommentPassedTime;
    }
}
