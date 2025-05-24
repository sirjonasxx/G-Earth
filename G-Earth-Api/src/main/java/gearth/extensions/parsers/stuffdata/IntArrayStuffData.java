package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

import java.util.Arrays;

// 5
public class IntArrayStuffData extends ArrayStuffData<Integer> {
    public final static int IDENTIFIER = 5;

    protected IntArrayStuffData() {}

    public IntArrayStuffData(Integer... values) {
        super();
        this.values = Arrays.asList(values);
    }

    public IntArrayStuffData(int uniqueSerialNumber, int uniqueSerialSize, Integer... values) {
        super(uniqueSerialNumber, uniqueSerialSize);
        this.values = Arrays.asList(values);
    }

    @Override
    protected void initialize(HPacket packet) {
        int size = packet.readInteger();
        this.clear();
        for (int i = 0; i < size; i++) {
            this.add(packet.readInteger());
        }
        super.initialize(packet);
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendInt(IDENTIFIER | this.getFlags());
        packet.appendInt(this.size());
        for (int i : this) {
            packet.appendInt(i);
        }
        super.appendToPacket(packet);
    }

    @Override
    public String getLegacyString() {
        return this.size() > 0 ? String.valueOf(this.get(0)) : "";
    }

    @Override
    public void setLegacyString(String legacyString) {
        if (this.size() > 0)
            this.set(0, Integer.parseInt(legacyString));
    }
}
