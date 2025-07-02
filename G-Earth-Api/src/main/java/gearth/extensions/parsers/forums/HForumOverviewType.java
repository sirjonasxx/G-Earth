package gearth.extensions.parsers.forums;

public enum HForumOverviewType {
    MOST_ACTIVE(0),
    MOST_READ(1),
    MY_FORUMS(2);

    private final int val;
    HForumOverviewType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static HForumOverviewType fromValue(int state) {
        switch (state) {
            case 0:
                return MOST_ACTIVE;
            case 1:
                return MOST_READ;
            case 2:
                return MY_FORUMS;
        }
        return MY_FORUMS;
    }
}
