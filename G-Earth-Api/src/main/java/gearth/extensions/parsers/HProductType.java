package gearth.extensions.parsers;

public enum HProductType {
    WallItem("I"),
    FloorItem("S"),
    Effect("E"),
    Badge("B");

    private String id;

    HProductType(String id) {
        this.id = id;
    }

    public String toString() {
        return id;
    }

    public static HProductType fromString(String id) {
        for (HProductType t : HProductType.values()) {
            if (t.toString().equalsIgnoreCase(id.toLowerCase())) return t;
        }
        return null;
    }
}
