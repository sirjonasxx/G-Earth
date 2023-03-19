package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;
import org.json.JSONObject;

public abstract class StuffDataBase implements IStuffData {
    private int flags = 0;
    private int uniqueSerialNumber = 0;
    private int uniqueSerialSize = 0;

    protected StuffDataBase() {}

    protected StuffDataBase(int uniqueSerialNumber, int uniqueSerialSize) {
        this.uniqueSerialNumber = uniqueSerialNumber;
        this.uniqueSerialSize = uniqueSerialSize;
        flags = 256;
    }

    protected void initialize(HPacket packet) {
        if ((flags & 256) > 0) {
            this.uniqueSerialNumber = packet.readInteger();
            this.uniqueSerialSize = packet.readInteger();
        }
    }

    public void appendToPacket(HPacket packet) {
        if ((flags & 256) > 0) {
            packet.appendInt(this.uniqueSerialNumber);
            packet.appendInt(this.uniqueSerialSize);
        }
    }

    @Override
    public String getLegacyString() {
        return "";
    }

    @Override
    public void setLegacyString(String legacyString) {}

    @Override
    public final void setFlags(int flags) {
        this.flags = flags;
    }

    @Override
    public final int getFlags() {
        return this.flags;
    }

    @Override
    public final int getUniqueSerialNumber() {
        return this.uniqueSerialNumber;
    }

    @Override
    public final int getUniqueSerialSize() {
        return this.uniqueSerialSize;
    }

    @Override
    public final void setUniqueSerialNumber(int uniqueSerialNumber) {
        this.uniqueSerialNumber = uniqueSerialNumber;
    }

    @Override
    public final void setUniqueSerialSize(int uniqueSerialSize) {
        this.uniqueSerialSize = uniqueSerialSize;
    }

    @Override
    public int getRarityLevel() {
        return -1;
    }

    @Override
    public final int getState() {
        try {
            return Integer.parseInt(getLegacyString());
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public final void setState(int state) {
        setLegacyString(String.valueOf(state));
    }

    @Override
    public final String getJSONValue(String key) {
        try {
            return new JSONObject(getLegacyString()).getString(key);
        } catch (Exception e) {
            return "";
        }
    }
}
