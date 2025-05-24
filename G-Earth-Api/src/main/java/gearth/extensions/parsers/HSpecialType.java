package gearth.extensions.parsers;

public enum HSpecialType {
    Default (1),
    WallPaper (2),
    FloorPaint (3),
    Landscape (4),
    PostIt (5),
    Poster (6),
    SoundSet (7),
    TraxSong (8),
    Present (9),
    EcotronBox (10),
    Trophy (11),
    CreditFurni (12),
    PetShampoo (13),
    PetCustomPart (14),
    PetCustomPartShampoo (15),
    PetSaddle (16),
    GuildFurni (17),
    GameFurni (18),
    MonsterplantSeed (19),
    MonsterplantRevival (20),
    MonsterplantRebreed (21),
    MonsterplantFertilize (22),
    FigurePurchasableSet (23);

    private final int id;

    HSpecialType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static HSpecialType fromId(int id) {
        for(HSpecialType value : HSpecialType.values()) {
            if(value.id == id) return value;
        }
        return null;
    }
}
