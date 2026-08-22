package gearth.extensions.parsers.forums;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

public class HForumOverview {

    private final HForumOverviewType viewMode;
    private final int size;
    private final int startIndex;
    private final List<HForum> forums;

    public HForumOverview(HPacket hPacket) {
        viewMode = HForumOverviewType.fromValue(hPacket.readInteger());
        size = hPacket.readInteger();
        startIndex = hPacket.readInteger();

        forums = new ArrayList<>();
        int forumsPageSize = hPacket.readInteger();
        for (int i = 0; i < forumsPageSize; i++) {
            forums.add(new HForum(hPacket));
        }
    }

    public HForumOverviewType getViewMode() {
        return viewMode;
    }

    public int getSize() {
        return size;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public List<HForum> getForums() {
        return forums;
    }
}
