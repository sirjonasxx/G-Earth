package wasm.disassembly.types;

import java.util.HashMap;
import java.util.Map;

public enum Mutability {
    CONST(0x00),
    VAR(0x01);

    public int val;
    Mutability(int val) {
        this.val = val;
    }

    private static Map<Integer, Mutability> map = new HashMap<>();
    static {
        for (Mutability valType : Mutability.values()) {
            map.put(valType.val, valType);
        }
    }

    public static Mutability from_val(int val) {
        return map.get(val);
    }

}
