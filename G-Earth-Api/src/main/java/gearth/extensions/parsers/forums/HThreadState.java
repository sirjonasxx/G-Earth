package gearth.extensions.parsers.forums;

public enum HThreadState {
    OPEN(0),
    HIDDEN_BY_ADMIN(10),
    HIDDEN_BY_STAFF(20);

    public final int state;

    HThreadState(int state) {
        this.state = state;
    }

    public static HThreadState fromValue(int state) {
        switch (state) {
            case 0:
                return OPEN;
            case 10:
                return HIDDEN_BY_ADMIN;
            case 20:
                return HIDDEN_BY_STAFF;
        }

        return OPEN;
    }
}