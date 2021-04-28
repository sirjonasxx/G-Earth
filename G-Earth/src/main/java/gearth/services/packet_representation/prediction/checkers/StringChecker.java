package gearth.services.packet_representation.prediction.checkers;

import gearth.protocol.HPacket;

import java.nio.charset.StandardCharsets;

public class StringChecker extends TypeChecker<String> {

    StringChecker(HPacket hPacket) {
        super("s", hPacket);
    }

    @Override
    public boolean canRead(int index) {
        return canReadString(hPacket, index);
    }

    @Override
    public double score(int index) {
        return Math.max(scoreString(get(index)) - (((double)index) / 1000000000000.0), 0);
    }

    @Override
    public String get(int index) {
        return hPacket.readString(index);
    }

    @Override
    int nextIndexSafe(int index) {
        return index + get(index).length() + 2;
    }

    // 2 = very common
    // 1 = a bit common
    // 0 = rare
    public static int isCommon(char c, byte b) {
        if (c == '\n' || c == '\r' ||c == '\t') {
            return 1;
        }

        int uByte = (((int)b) + 256) % 256;

        if (uByte >= 32 && uByte <= 126) {
            return 2;
        }
        return 0;
    }

    public static double scoreString(String s) {
        byte[] asBytes = s.getBytes(StandardCharsets.ISO_8859_1);
        char[] asChars = s.toCharArray();

        if (s.equals("")) {
            return 0.8;
        }

        int len = s.length();
        double score = 1;

        double[] penalties = new double[]{
                (-1.0/(len*0.3+2) + 0.5),
                (-1.0/(len+2)) + 1,
                s.length() == 1 ? 0.9 : (s.length() == 2 ? 0.98 : 1.0)
        };

        for (int i = 0; i < s.length(); i++) {

            // detect UTF8 extended chars
            if ((asBytes[i] & 0b11100000) == 0b11000000 && i < s.length() - 1 && (asBytes[i+1] & 0b11000000) == 0b10000000) {
                i += 1;
                score *= penalties[2]*penalties[2];
            }
            else if ((asBytes[i] & 0b11110000) == 0b11100000 && i < s.length() - 2 && (asBytes[i+1] & 0b11000000) == 0b10000000 && (asBytes[i+2] & 0b11000000) == 0b10000000) {
                i += 2;
                score *= penalties[2]*penalties[2]*penalties[2];
            }
            else if ((asBytes[i] & 0b11111000) == 0b11110000 && i < s.length() - 3 && (asBytes[i+1] & 0b11000000) == 0b10000000 && (asBytes[i+2] & 0b11000000) == 0b10000000 && (asBytes[i+3] & 0b11000000) == 0b10000000) {
                i += 3;
                score *= penalties[2]*penalties[2]*penalties[2]*penalties[2];
            }
            else {
                score *= penalties[isCommon(
                        asChars[i],
                        asBytes[i]
                )];

                if (score < 0.001) {
                    return 0;
                }
            }
        }

        return score;
    }

    public static boolean canReadString(HPacket packet, int index) {
        int l = packet.getBytesLength();
        return index >= 6 && !(index + 2 > l || packet.readUshort(index) + 2 + index > l);
    }
}
