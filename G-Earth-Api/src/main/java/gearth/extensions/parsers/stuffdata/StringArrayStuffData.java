package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

// 2
public class StringArrayStuffData extends ArrayStuffData<String> {
    public final static int IDENTIFIER = 2;

    protected StringArrayStuffData() {}

    public StringArrayStuffData(String... array) {
        this.values = Arrays.stream(array).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public StringArrayStuffData(int uniqueSerialNumber, int uniqueSerialSize, String... array) {
        super(uniqueSerialNumber, uniqueSerialSize);
        this.values = Arrays.stream(array).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    protected void initialize(HPacket packet) {
        int size = packet.readInteger();
        this.clear();
        for (int i = 0; i < size; i++) {
            this.add(packet.readString());
        }
        super.initialize(packet);
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendInt(IDENTIFIER | this.getFlags());
        packet.appendInt(this.size());
        for (String s : this) {
            packet.appendString(s);
        }
        super.appendToPacket(packet);
    }

    @Override
    public String getLegacyString() {
        return this.size() > 0 ? this.get(0) : "";
    }

    @Override
    public void setLegacyString(String legacyString) {
        if (this.size() > 0) {
            this.set(0, legacyString == null ? "" : legacyString);
        }
    }
}
