package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

public class CrackableStuffData extends StuffDataBase {
    public final static int IDENTIFIER = 7;

    private String legacyString = "";
    private int hits = 0;
    private int target = 0;

    protected CrackableStuffData() {}

    public CrackableStuffData(String legacyString, int hits, int target) {
        super();
        this.legacyString = legacyString == null ? "" : legacyString;
        this.hits = hits;
        this.target = target;
    }


    public CrackableStuffData(int uniqueSerialNumber, int uniqueSerialSize, String legacyString, int hits, int target) {
        super(uniqueSerialNumber, uniqueSerialSize);
        this.legacyString = legacyString == null ? "" : legacyString;
        this.hits = hits;
        this.target = target;
    }

    @Override
    protected void initialize(HPacket packet) {
        this.legacyString = packet.readString();
        this.hits = packet.readInteger();
        this.target = packet.readInteger();
        super.initialize(packet);
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendInt(IDENTIFIER | this.getFlags());
        packet.appendObjects(
                this.legacyString,
                this.hits,
                this.target
        );
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

    public int getHits() {
        return this.hits;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public int getTarget() {
        return this.target;
    }

    public void setTarget(int target) {
        this.target = target;
    }
}
