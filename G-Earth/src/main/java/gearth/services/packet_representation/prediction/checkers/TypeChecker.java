package gearth.services.packet_representation.prediction.checkers;

import gearth.protocol.HPacket;

public abstract class TypeChecker<T> {

    protected String structCode;
    protected HPacket hPacket;

    protected TypeChecker(String structCode, HPacket hPacket) {
        this.structCode = structCode;
        this.hPacket = hPacket;
    }

    public abstract boolean canRead(int index);

    public abstract double score(int index);

    abstract T get(int index);

    // -1 if cant read
    public int nextIndex(int index) {
        if (!canRead(index)) return -1;
        return nextIndexSafe(index);
    }

    abstract int nextIndexSafe(int index);

    public String getStructCode() {
        return structCode;
    }

}
