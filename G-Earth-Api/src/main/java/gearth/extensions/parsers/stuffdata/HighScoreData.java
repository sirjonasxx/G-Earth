package gearth.extensions.parsers.stuffdata;

import gearth.protocol.HPacket;

import java.util.Arrays;
import java.util.Objects;

public class HighScoreData {
    private int score;
    private String[] users;

    protected HighScoreData(HPacket packet) {
        this.score = packet.readInteger();
        int size = packet.readInteger();
        this.users = new String[size];
        for (int i = 0; i < size; i++) {
            this.users[i] = packet.readString();
        }
    }

    public HighScoreData(int score, String... users) {
        super();
        this.score = score;
        this.users = Arrays.stream(users).filter(Objects::nonNull).toArray(String[]::new);
    }

    protected void appendToPacket(HPacket packet) {
        packet.appendInt(this.score);
        packet.appendInt(this.users.length);
        for (String user : this.users) {
            packet.appendString(user);
        }
    }

    public int getScore() {
        return this.score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String[] getUsers() {
        return this.users.clone();
    }

    public void setUsers(String[] users) {
        this.users = Arrays
                .stream(users == null ? new String[0] : users)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }
}
