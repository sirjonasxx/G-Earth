package gearth.services.packet_representation.prediction.checkers;

import gearth.protocol.HPacket;

public class ShortChecker extends TypeChecker {

    private BooleanChecker booleanChecker;
    private ByteChecker byteChecker;

    protected ShortChecker(HPacket hPacket) {
        super("u", hPacket);
        booleanChecker = new BooleanChecker(hPacket);
        byteChecker = new ByteChecker(hPacket);
    }

    @Override
    public boolean canRead(int index) {
        return index >= 6 && !(index + 2 > hPacket.getBytesLength());
    }

    @Override
    public double score(int index) {
        short val = hPacket.readShort(index);

        if (index == 6 && val == 0 && hPacket.length() == nextIndexSafe(index)) {
            return 1;
        }

        if (val <= 0) {
            return 0;
        }

        if (val < 1000) {
            return 0.5;
        }

        double leftMinScore;
        double rightMinScore;
        if (booleanChecker.canRead(index)) leftMinScore = booleanChecker.score(index);
        else leftMinScore = byteChecker.score(index);
        if (booleanChecker.canRead(index+1)) rightMinScore = booleanChecker.score(index+1);
        else rightMinScore = byteChecker.score(index+1);

        return leftMinScore * rightMinScore + 0.00000001;
    }

    @Override
    Object get(int index) {
        return hPacket.readShort();
    }

    @Override
    int nextIndexSafe(int index) {
        return index + 2;
    }
}
