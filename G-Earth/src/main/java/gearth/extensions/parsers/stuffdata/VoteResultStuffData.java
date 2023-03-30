package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

public class VoteResultStuffData extends StuffDataBase {
    public static final int IDENTIFIER = 3;

    private String legacyString = "";
    private int result = 0;

    protected VoteResultStuffData() {}

    public VoteResultStuffData(String legacyString, int result) {
        this.legacyString = legacyString == null ? "" : legacyString;
        this.result = result;
    }

    public VoteResultStuffData(int uniqueSerialNumber, int uniqueSerialSize, String legacyString, int result) {
        super(uniqueSerialNumber, uniqueSerialSize);
        this.legacyString = legacyString == null ? "" : legacyString;
        this.result = result;
    }

    @Override
    protected void initialize(HPacket packet) {
        this.legacyString = packet.readString();
        this.result = packet.readInteger();
        super.initialize(packet);
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendInt(IDENTIFIER | this.getFlags());
        packet.appendObjects(
                this.legacyString,
                this.result
        );
        super.appendToPacket(packet);
    }

    @Override
    public String getLegacyString() {
        return this.legacyString;
    }

    @Override
    public void setLegacyString(String legacyString) {
        this.legacyString = legacyString == null ? "" : legacyString;
    }

    public int getResult() {
        return this.result;
    }

    public void setResult(int result) {
        this.result = result;
    }
}
