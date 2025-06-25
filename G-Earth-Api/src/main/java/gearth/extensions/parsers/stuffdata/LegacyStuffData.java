package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

public class LegacyStuffData extends StuffDataBase {
    public final static int IDENTIFIER = 0;

    private String legacyString;

    protected LegacyStuffData() {}

    public LegacyStuffData(String legacyString) {
        this.legacyString = legacyString == null ? "" : legacyString;
    }

    public LegacyStuffData(int uniqueSerialNumber, int uniqueSerialSize, String legacyString) {
        super(uniqueSerialNumber, uniqueSerialSize);
        this.legacyString = legacyString == null ? "" : legacyString;
    }

    @Override
    protected void initialize(HPacket packet) {
        this.legacyString = packet.readString();
        super.initialize(packet);
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendInt(IDENTIFIER | this.getFlags());
        packet.appendString(this.legacyString);
        super.appendToPacket(packet);
    }

    @Override
    public String getLegacyString() {
        return this.legacyString;
    }

    @Override
    public void setLegacyString(String legacyString) {
        this.legacyString = legacyString;
    }
}
