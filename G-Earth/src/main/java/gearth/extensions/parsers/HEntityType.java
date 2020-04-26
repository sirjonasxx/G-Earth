package gearth.extensions.parsers;

import java.util.HashMap;
import java.util.Map;

public enum HEntityType {
    HABBO(1),
    PET(2),
    OLD_BOT(3),
    BOT(4);

    private int id;

    HEntityType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }


    private static Map<Integer, HEntityType> map = new HashMap<>();

    static {
        for (HEntityType type : HEntityType.values()) {
            map.put(type.id, type);
        }
    }

    public static HEntityType valueOf (int id) {
        return map.get(id);
    }
}
