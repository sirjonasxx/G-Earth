package gearth.extensions.parsers;

public enum HGender {
    Unisex("U"),
    Male("M"),
    Female("F");

    private final String id;

    HGender(String id) {
        this.id = id;
    }

    public String toString() {
        return id;
    }
}
