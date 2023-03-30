package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

import java.util.Arrays;
import java.util.Objects;

public class HighScoreStuffData extends StuffDataBase {
    public final static int IDENTIFIER = 6;

    private String legacyString = "";
    // ['perteam', 'mostwins', 'classic', 'fastesttime', 'longesttime']
    private int scoreType = 0;
    // ['alltime', 'daily', 'weekly', 'monthly']
    private int clearType = 0;
    private HighScoreData[] entries = {};

    protected HighScoreStuffData() {}

    public HighScoreStuffData(String legacyString, int scoreType, int clearType, HighScoreData... entries) {
        super();
        this.legacyString = legacyString == null ? "" : legacyString;
        this.scoreType = scoreType;
        this.clearType = clearType;
        this.entries = Arrays.stream(entries).filter(Objects::nonNull).toArray(HighScoreData[]::new);
    }

    public HighScoreStuffData(int uniqueSerialNumber, int uniqueSerialSize, String legacyString, int scoreType, int clearType, HighScoreData... entries) {
        super(uniqueSerialNumber, uniqueSerialSize);
        this.legacyString = legacyString == null ? "" : legacyString;
        this.scoreType = scoreType;
        this.clearType = clearType;
        this.entries = Arrays.stream(entries).filter(Objects::nonNull).toArray(HighScoreData[]::new);
    }

    @Override
    protected void initialize(HPacket packet) {
        this.legacyString = packet.readString();
        this.scoreType = packet.readInteger();
        this.clearType = packet.readInteger();

        int size = packet.readInteger();
        this.entries = new HighScoreData[size];
        for (int i = 0; i < size; i++) {
            this.entries[i] = new HighScoreData(packet);
        }
    }

    @Override
    public void appendToPacket(HPacket packet) {
        packet.appendInt(IDENTIFIER | this.getFlags());
        packet.appendObjects(
                this.legacyString,
                this.scoreType,
                this.clearType,
                this.entries.length
        );
        for (HighScoreData entry : this.entries) {
            entry.appendToPacket(packet);
        }
    }

    @Override
    public String getLegacyString() {
        return this.legacyString;
    }

    @Override
    public void setLegacyString(String legacyString) {
        this.legacyString = legacyString;
    }

    public int getScoreType() {
        return this.scoreType;
    }

    public void setScoreType(int scoreType) {
        this.scoreType = scoreType;
    }

    public int getClearType() {
        return this.clearType;
    }

    public void setClearType(int clearType) {
        this.clearType = clearType;
    }

    public HighScoreData[] getEntries() {
        return this.entries.clone();
    }

    public void setEntries(HighScoreData[] entries) {
        this.entries = Arrays
                .stream(entries == null ? new HighScoreData[0] : entries)
                .filter(Objects::nonNull)
                .toArray(HighScoreData[]::new);
    }
}
