package gearth.extensions.parsers;

public enum HRelationshipStatus {
    NONE (0),
    HEART (1),
    SMILEY (2),
    SKULL (3);

    private final int id;

    HRelationshipStatus(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static HRelationshipStatus fromId(int id) {
        for(HRelationshipStatus status : HRelationshipStatus.values()) {
            if(status.id == id) return status;
        }
        return null;
    }
}
