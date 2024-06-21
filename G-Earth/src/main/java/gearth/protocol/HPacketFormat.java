package gearth.protocol;

import gearth.protocol.packethandler.shockwave.packets.ShockPacketIncoming;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketOutgoing;

public enum HPacketFormat {

    EVA_WIRE,
    WEDGIE_INCOMING,
    WEDGIE_OUTGOING;

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
