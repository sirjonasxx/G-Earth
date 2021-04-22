package gearth.misc.packet_info;

import gearth.protocol.HMessage;

public class PacketInfo {
    private final HMessage.Direction destination;
    private final int headerId;
    private final String hash;
    private final String name;
    private final String structure;

    public PacketInfo(HMessage.Direction destination, int headerId, String hash, String name, String structure) {
        this.destination = destination;
        this.headerId = headerId;
        this.hash = hash;
        this.name = name;
        this.structure = structure;
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public int getHeaderId() {
        return headerId;
    }

    public HMessage.Direction getDestination() {
        return destination;
    }

    public String getStructure() {
        return structure;
    }

    public String toString() {
        return headerId + ": " + "[" + name + "][" + structure + "]";
    }
}