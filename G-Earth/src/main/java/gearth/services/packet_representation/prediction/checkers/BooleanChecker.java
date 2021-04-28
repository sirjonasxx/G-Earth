package gearth.services.packet_representation.prediction.checkers;

import gearth.protocol.HPacket;

public class BooleanChecker extends TypeChecker<Boolean> {

    BooleanChecker(HPacket hPacket) {
        super("B", hPacket);
    }

    @Override
    public boolean canRead(int index) {
        return index >= 6 && index < hPacket.getBytesLength() &&
                (hPacket.toBytes()[index] == 1 || hPacket.toBytes()[index] == 0);
    }

    @Override
    public double score(int index) {
        // A [0] byte is not very likely to be a boolean
        // A [1] byte is not very likely to be a boolean, but a little bit more

        // 0.6 may seem pretty high but 4 bytes in a row would get score 0.6*0.6*0.6*0.6, which is low
        return get(index) ? 0.6 : 0.5;
    }

    @Override
    public Boolean get(int index) {
        return hPacket.readBoolean(index);
    }

    @Override
    int nextIndexSafe(int index) {
        return index + 1;
    }

}
