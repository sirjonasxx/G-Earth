package wasm.disassembly.types;

import java.util.HashMap;
import java.util.Map;

public enum ElemType {
    FUNCREF(0x70);


    public int val;
    ElemType(int val) {
        this.val = val;
    }

    private static Map<Integer, ElemType> map = new HashMap<>();
    static {
        for (ElemType valType : ElemType.values()) {
            map.put(valType.val, valType);
        }
    }

    public static ElemType from_val(int val) {
        return map.get(val);
    }

}
