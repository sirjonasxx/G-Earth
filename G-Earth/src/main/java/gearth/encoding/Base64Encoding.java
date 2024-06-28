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

        for (int k = data.length - 1, i = 0; k >= 0; k--, i++)
        {
            int x = data[k] - 0x40;
            if (i > 0){
                res += x << (i * 6);
            } else {
                res += x;
            }
        }

        return res;
    }
}
