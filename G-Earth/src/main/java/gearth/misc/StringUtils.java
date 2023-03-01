package gearth.misc;

/**
 * Contains utility methods for {@link String} conversions.
 */
public final class StringUtils {

    /**
     * Interprets the argued {@link String} as a hex-string and converts it to a byte array.
     * @param hexString the {@link String} to be converted.
     * @return a byte array containing the converted hex string values.
     */
    public static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }
}
