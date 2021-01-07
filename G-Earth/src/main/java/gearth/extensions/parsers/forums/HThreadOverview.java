package gearth.extensions.parsers.forums;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

public class HThreadOverview {

    private final int guildId;
    private final int startIndex;
    private final List<HThread> threads;

    public HThreadOverview(HPacket hPacket) {
        guildId = hPacket.readInteger();
        startIndex = hPacket.readInteger();

        threads = new ArrayList<>();
        int threadsSize = hPacket.readInteger();
        for (int i = 0; i < threadsSize; i++) {
            threads.add(new HThread(hPacket));
        }
    }

    public int getGuildId() {
        return guildId;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public List<HThread> getThreads() {
        return threads;
    }
}
