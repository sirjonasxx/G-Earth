package gearth.extensions.parsers.forums;

import gearth.protocol.HPacket;

public class HComment {

    private final int commentId;
    private final int indexInThread;
    private final int userId;
    private final String userName;
    private final String look;
    private final int passedTime;
    private final String message;
    private final HThreadState state;
    private final int adminId;
    private final String adminName;

    private final int irrelevantId;
    private final int authorPostCount;

    public HComment(HPacket hPacket) {
        commentId = hPacket.readInteger();
        indexInThread = hPacket.readInteger();
        userId = hPacket.readInteger();
        userName = hPacket.readString();
        look = hPacket.readString();
        passedTime = hPacket.readInteger();
        message = hPacket.readString();
        state = HThreadState.fromValue(hPacket.readByte());
        adminId = hPacket.readInteger();
        adminName = hPacket.readString();
        irrelevantId = hPacket.readInteger();
        authorPostCount = hPacket.readInteger();
    }

    public int getCommentId() {
        return commentId;
    }

    public int getIndexInThread() {
        return indexInThread;
    }

    public int getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getLook() {
        return look;
    }

    public int getPassedTime() {
        return passedTime;
    }

    public String getMessage() {
        return message;
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

    public int getIrrelevantId() {
        return irrelevantId;
    }

    public int getAuthorPostCount() {
        return authorPostCount;
    }
}
