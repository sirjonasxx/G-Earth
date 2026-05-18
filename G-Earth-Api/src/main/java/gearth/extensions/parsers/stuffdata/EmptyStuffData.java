package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

public class EmptyStuffData extends StuffDataBase {
    public final static int IDENTIFIER = 4;

    public EmptyStuffData() {
        super();
    }

    public EmptyStuffData(int uniqueSerialNumber, int uniqueSerialSize) {
        super(uniqueSerialNumber, uniqueSerialSize);
    }

    @Override
    protected void initialize(HPacket packet) {
        super.initialize(packet);
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendInt(IDENTIFIER | this.getFlags());
        super.appendToPacket(packet);
    }
}
