package gearth.extensions.parsers.navigator;

import gearth.protocol.HPacket;

import java.util.*;

public class HNavigatorSearchResult {
    private String searchCode;
    private String filteringData;
    private final List<HNavigatorBlock> blocks = new ArrayList<>();

    public HNavigatorSearchResult(HPacket packet) {
        this.searchCode = packet.readString();
        this.filteringData = packet.readString();

        int count = packet.readInteger();
        for(int i = 0; i < count; i++) {
            blocks.add(new HNavigatorBlock(packet));
        }
    }

    public void appendToPacket(HPacket packet) {
        packet.appendString(this.searchCode);
        packet.appendString(this.filteringData);

        packet.appendInt(blocks.size());
        synchronized (blocks) {
            for(HNavigatorBlock block : blocks) {
                block.appendToPacket(packet);
            }
        }
    }

    public String getSearchCode() {
        return searchCode;
    }

    public void setSearchCode(String searchCode) {
        this.searchCode = searchCode;
    }

    public String getFilteringData() {
        return filteringData;
    }

    public void setFilteringData(String filteringData) {
        this.filteringData = filteringData;
    }

    public List<HNavigatorBlock> getBlocks() {
        synchronized (blocks) {
            return Collections.unmodifiableList(blocks);
        }
    }

    public void removeBlocks(HNavigatorBlock... removingBlocks) {
        synchronized (blocks) {
            blocks.removeAll(Arrays.asList(removingBlocks));
        }
    }

    public HNavigatorBlock removeBlock(int index) {
        synchronized (blocks) {
            return blocks.remove(index);
        }
    }

    public void addBlocks(HNavigatorBlock... addingBlocks) {
        synchronized (blocks) {
            blocks.addAll(Arrays.asList(addingBlocks));
        }
    }
}
