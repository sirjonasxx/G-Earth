package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

public interface IStuffData {
    static IStuffData read(HPacket packet) {
        int a = packet.readInteger();
        StuffDataBase stuffData = null;

        switch (a & 255) {
            case LegacyStuffData.IDENTIFIER:
                stuffData = new LegacyStuffData();
                break;
            case MapStuffData.IDENTIFIER:
                stuffData = new MapStuffData();
                break;
            case StringArrayStuffData.IDENTIFIER:
                stuffData = new StringArrayStuffData();
                break;
            case VoteResultStuffData.IDENTIFIER:
                stuffData = new VoteResultStuffData();
                break;
            case EmptyStuffData.IDENTIFIER:
                stuffData = new EmptyStuffData();
                break;
            case IntArrayStuffData.IDENTIFIER:
                stuffData = new IntArrayStuffData();
                break;
            case HighScoreStuffData.IDENTIFIER:
                stuffData = new HighScoreStuffData();
                break;
            case CrackableStuffData.IDENTIFIER:
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
