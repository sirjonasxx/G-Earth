package gearth.services.packet_representation.prediction.checkers;

import gearth.protocol.HPacket;

import java.nio.charset.StandardCharsets;

public class IntegerChecker extends TypeChecker<Integer> {

    IntegerChecker(HPacket hPacket) {
        super("i", hPacket);
    }

    @Override
    public boolean canRead(int index) {
        return index >= 6 && !(index + 4 > hPacket.getBytesLength());
    }

    @Override
    public double score(int index) {
        int value = get(index);


        int ushortTest = hPacket.readUshort(index);
        int ushortTest2 = hPacket.readUshort(index + 2);
        // if 4 bytes reads like xy[0][0] or xy[0][1], it is very unlikely to be an actual integer
        if (ushortTest != 0 && (ushortTest2 == 0 || ushortTest2 == 256 || ushortTest2 == 1 || ushortTest2 == 257)) {
            return 0.05;
        }

        // 4 bytes that read [0][0][1][0] are also unlikely to be an integer
        if (value == 256) {
            return 0.04;
        }

        // 4 bytes that read [0][2]xy could be a string
        if (ushortTest == 2 && StringChecker.canReadString(hPacket, index)) {
            return (1 - StringChecker.scoreString(hPacket.readString(index)));
        }

        // if 4 bytes read "abcd", it will most likely be part of a string
        // so check if bytes are common
        byte[] asBytes = hPacket.readBytes(4, index);
        char[] asChars = new String(asBytes, StandardCharsets.ISO_8859_1).toCharArray();
        int count = 0;
        for (int i = 0; i < 4; i++) {
            if (StringChecker.isCommon(asChars[i], asBytes[i]) == 2) {
                count++;
            }
        }
        if (count == 4) {
            return 0.2;
        }

        // also this
        if (StringChecker.canReadString(hPacket, index - 1)) {
            String s = hPacket.readString(index - 1);
            if (s.length() > 2 && s.length() < 10) {
                if (StringChecker.scoreString(s) > 0.5) {
                    return 1 - StringChecker.scoreString(s);
                }
            }
        }

        // when ordering, it often appears that integers are placed before strings/booleans/etc
        // in the case of empty strings or false booleans, it is as good as always the integer that comes first
        // so we'll try to respect that here with a small score adjust, which doesnt affect anything else than ordering
        double offset = ((double)index) / 1000000000.0;

        // since -1 has a byte arrangement barely used by other packets, we can assign full score
        if (value == -1) {
            return 1 - offset;
        }

        if (value == 0) {
            return 0.99 - offset;
        }

        // if the value is not 0, but the last byte is 0/1, we assign a less score
        // to keep the possibility open for a boolean
        if (value % 256 == 0) {
            return 0.06 - offset;
        }
        if (value != 1 && value % 256 == 1) {
            return 0.06 - offset;
        }

        if (value >= -1) {
            return 0.92 - offset;
        }

        return 0.8 - offset;
    }

    @Override
    public Integer get(int index) {
        return hPacket.readInteger(index);
    }

    @Override
    int nextIndexSafe(int index) {
        return index + 4;
    }
}
