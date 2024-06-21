package gearth.protocol.format.shockwave;

import gearth.protocol.HMessage;

public class ShockMessage {

    private final ShockPacket packet;
    private final HMessage.Direction direction;
    private final int index;

    private boolean isBlocked;

    public ShockMessage(ShockPacket packet, HMessage.Direction direction, int index) {
        this.packet = packet;
        this.direction = direction;
        this.index = index;
        this.isBlocked = false;
    }

    public ShockPacket getPacket() {
        return packet;
    }

    public HMessage.Direction getDirection() {
        return direction;
    }

    public int getIndex() {
        return index;
    }

    public boolean isBlocked() {
        return isBlocked;
    }
}
