package gearth.protocol;

import gearth.protocol.connection.HClient;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketIncoming;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketOutgoing;

import java.util.HashMap;
import java.util.Map;

public enum HPacketFormat {

    EVA_WIRE(0),
    WEDGIE_INCOMING(1),
    WEDGIE_OUTGOING(2);

    private static final Map<Integer, HPacketFormat> ID_MAP = new HashMap<>();

    static {
        for (HPacketFormat format : values()) {
            ID_MAP.put(format.id, format);
        }
    }

    public static HPacketFormat fromId(int id) {
        return ID_MAP.get(id);
    }

    private final int id;

    HPacketFormat(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static HPacketFormat getFormat(HClient client, HMessage.Direction direction) {
        if (client != HClient.SHOCKWAVE) {
            return EVA_WIRE;
        } else {
            return direction == HMessage.Direction.TOCLIENT ? WEDGIE_INCOMING : WEDGIE_OUTGOING;
        }
    }

    public HPacket createPacket(String data) {
        switch (this) {
            case EVA_WIRE:
                return new HPacket(data);
            case WEDGIE_INCOMING:
                return new ShockPacketIncoming(data);
            case WEDGIE_OUTGOING:
                return new ShockPacketOutgoing(data);
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public HPacket createPacket(int headerId) {
        switch (this) {
            case EVA_WIRE:
                return new HPacket(headerId);
            case WEDGIE_INCOMING:
                return new ShockPacketIncoming(headerId);
            case WEDGIE_OUTGOING:
                return new ShockPacketOutgoing(headerId);
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public HPacket createPacket(byte[] packet) {
        switch (this) {
            case EVA_WIRE:
                return new HPacket(packet);
            case WEDGIE_INCOMING:
                return new ShockPacketIncoming(packet);
            case WEDGIE_OUTGOING:
                return new ShockPacketOutgoing(packet);
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
