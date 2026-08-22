package gearth.extensions.parsers.forums;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

public class HCommentOverview {

    private final int guildId;
    private final int threadId;
    private final int startIndex;
    private final List<HComment> comments;


    public HCommentOverview(HPacket packet) {
        guildId = packet.readInteger();
        threadId = packet.readInteger();
        startIndex = packet.readInteger();

        comments = new ArrayList<>();
        int length = packet.readInteger();
        for (int i = 0; i < length; i++) {
            comments.add(new HComment(packet));
        }
    }

    public int getGuildId() {
        return guildId;
    }

    public int getThreadId() {
        return threadId;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public List<HComment> getComments() {
        return comments;
    }
}
