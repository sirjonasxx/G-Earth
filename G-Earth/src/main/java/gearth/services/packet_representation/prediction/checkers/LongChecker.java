package gearth.services.packet_representation.prediction.checkers;

import gearth.protocol.HPacket;

public class LongChecker extends TypeChecker<Long> {

    private IntegerChecker integerChecker;

    protected LongChecker(HPacket hPacket) {
        super("l", hPacket);
        integerChecker = new IntegerChecker(hPacket);
    }

    @Override
    public boolean canRead(int index) {
        return index >= 6 && !(index + 8 > hPacket.getBytesLength());
    }

    @Override
    public double score(int index) {
        int split1 = hPacket.readInteger(index);
        int split2 = hPacket.readInteger(index + 4);

        int zeros = 0;
        for (int i = index + 4; i < index + 8; i++) {
            zeros += hPacket.readByte(i) == 0 ? 1 : 0;
        }

        if (split2 > 256 * 256 * 3 && split1 == 0 && zeros < 2) {
            return integerChecker.score(index) * integerChecker.score(index + 4) + 0.0000000001;
        }

        return 0;
    }

    @Override
    Long get(int index) {
        return hPacket.readLong(index);
    }

    @Override
    int nextIndexSafe(int index) {
        return index + 8;
    }
}
