package gearth.encoding;

/**
 * Kepler Copyright (C) 2018 Quackster
 * <a href="https://github.com/Quackster/Kepler">Kepler</a>
 */
public class Base64Encoding {

    public static byte[] encode(int i, int numBytes) {
        byte[] bzRes = new byte[numBytes];
        for (int j = 1; j <= numBytes; j++)
        {
            int k = ((numBytes - j) * 6);
            bzRes[j - 1] = (byte)(0x40 + ((i >> k) & 0x3f));
        }

        return bzRes;
    }

    public static int decode(byte[] data) {
        int res = 0;

        for (byte x : data) {
            final int byteVal = x - 0x40;

            res = (res << 6) | byteVal;
        }

        return res;
    }
}
