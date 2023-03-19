package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

public interface IStuffData {
    static IStuffData read(HPacket packet) {
        int a = packet.readInteger();
        StuffDataBase stuffData = null;

        switch (a & 255) {
            case 0:
                stuffData = new LegacyStuffData();
                break;
            case 1:
                stuffData = new MapStuffData();
                break;
            case 2:
                stuffData = new StringArrayStuffData();
                break;
            case 3:
                stuffData = new VoteResultStuffData();
                break;
            case 4:
                stuffData = new EmptyStuffData();
                break;
            case 5:
                stuffData = new IntArrayStuffData();
                break;
            case 6:
                stuffData = new HighScoreStuffData();
                break;
            case 7:
                stuffData = new CrackableStuffData();
                break;
        }

        if (stuffData != null) {
            stuffData.setFlags(a & 65280);
            stuffData.initialize(packet);
        } else {
            throw new RuntimeException("Unknown stuffdata type");
        }

        return stuffData;
    }

    void appendToPacket(HPacket packet);

    String getLegacyString();
    void setLegacyString(String legacyString);

    void setFlags(int flags);
    int getFlags();

    int getUniqueSerialNumber();
    int getUniqueSerialSize();
    void setUniqueSerialNumber(int uniqueSerialNumber);
    void setUniqueSerialSize(int uniqueSerialSize);

    int getRarityLevel();

    int getState();
    void setState(int state);

    String getJSONValue(String key);
}
