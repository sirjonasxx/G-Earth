package gearth.encoding;

import org.bouncycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

public class HexEncoding {

    public static byte[] toBytes(String s) {
        return Hex.decode(s);
    }

    public static byte[] toHex(byte[] bytes, boolean upperCase) {
        String data = Hex.toHexString(bytes);

        if (upperCase) {
            data = data.toUpperCase();
        }

        return data.getBytes(StandardCharsets.ISO_8859_1);
    }

}
