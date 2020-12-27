package gearth.misc.packetrepresentation.prediction.checkers;

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

        if (split2 > 256 * 256 * 3 && split1 == 0) {
            return integerChecker.score(index);
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
