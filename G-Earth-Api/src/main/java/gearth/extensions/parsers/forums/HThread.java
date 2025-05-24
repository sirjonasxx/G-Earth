package gearth.extensions.parsers.forums;

import gearth.protocol.HPacket;

public class HThread {

    private final int threadId;

    private final int authorId;
    private final String authorName;
    private final String subject;

    private final boolean pinned;
    private final boolean locked;

    private final int passedTime;
    private final int ammountComments;
    private final int unreadComments;

    private final int lastCommentIndexInForum;
    private final int lastCommentAuthorId;
    private final String lastCommentAuthorName;
    private final int lastCommentPassedTime;

    private final HThreadState state;
    private final int adminId;
    private final String adminName;

    private final int unknownThreadId;

    public HThread(HPacket hPacket) {
        threadId = hPacket.readInteger();

        authorId = hPacket.readInteger();
        authorName = hPacket.readString();
        subject = hPacket.readString();

        pinned = hPacket.readBoolean();
        locked = hPacket.readBoolean();

        passedTime = hPacket.readInteger();
        ammountComments = hPacket.readInteger();
        unreadComments = hPacket.readInteger();

        lastCommentIndexInForum = hPacket.readInteger();
        lastCommentAuthorId = hPacket.readInteger();
        lastCommentAuthorName = hPacket.readString();
        lastCommentPassedTime = hPacket.readInteger();

        state = HThreadState.fromValue(hPacket.readByte());
        adminId = hPacket.readInteger();
        adminName = hPacket.readString();

        unknownThreadId = hPacket.readInteger();
    }

    public int getThreadId() {
        return threadId;
    }

    public int getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getSubject() {
        return subject;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isLocked() {
        return locked;
    }

    public int getPassedTime() {
        return passedTime;
    }

    public int getAmmountComments() {
        return ammountComments;
    }

    public int getUnreadComments() {
        return unreadComments;
    }

    public int getLastCommentIndexInForum() {
        return lastCommentIndexInForum;
    }

    public int getLastCommentAuthorId() {
        return lastCommentAuthorId;
    }

    public String getLastCommentAuthorName() {
        return lastCommentAuthorName;
    }

    public int getLastCommentPassedTime() {
        return lastCommentPassedTime;
    }

    public HThreadState getState() {
        return state;
    }

    public int getAdminId() {
        return adminId;
    }

    public String getAdminName() {
        return adminName;
    }

    public int getUnknownThreadId() {
        return unknownThreadId;
    }
}
