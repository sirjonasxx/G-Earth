package main.protocol;

public class HMessage {

    public enum Side {
        TOSERVER,
        TOCLIENT
    }

    private HPacket hPacket;
    private Side side;
    private int index;

    private boolean isBlocked;


    public HMessage(HPacket packet, Side side, int index) {
        this.side = side;
        this.hPacket = packet;
        this.index = index;
        isBlocked = false;
    }

    public int getIndex() {
        return index;
    }

    public void setBlocked(boolean block) {
        isBlocked = block;
    }
    public boolean isBlocked() {
        return isBlocked;
    }

    public HPacket getPacket() {
        return hPacket;
    }
    public Side getDestination() {
        return side;
    }

    public boolean isCorrupted() {
        return hPacket.isCorrupted();
    }
}
