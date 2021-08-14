package gearth.services.internal_extensions.uilogger.hexdumper;

// https://github.com/zuckel/hexdump

/**
 * Utility class for generating hexdumps from byte arrays. Mostly for debugging purposes.
 */
public final class Hexdump {

    private static final char[] HEX =
            new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static final char NON_PRINTABLE = '\u25a1'; // WHITE SQUARE □

    private Hexdump() {
    }

    /**
     * <p>
     * Create human-readable hexdump for a byte array.
     * </p>
     * <p>
     * This method is not thread-safe in the sense that bytes will be read more than once, thus possibly producing inconsistent
     * output if the byte array is mutated concurrently.
     * </p>
     *
     * @param bytes array to be rendered
     * @return human-readable formatted hexdump as a String
     */
    public static String hexdump(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        if (bytes.length == 0) {
            return "empty";
        }

        StringBuilder out = new StringBuilder();

        for (int offset = 0; offset < bytes.length; offset += 16) {
            appendLine(bytes, offset, out);
        }
        appendOffset(bytes.length, out);
//        out.append('\n');

        return out.toString();
    }

    // 00000000  2f 2e 63 6c 61 73 73 70  61 74 68 0a 2f 2e 70 72  |/.classpath¶/.pr|
    //   offset              first block             second block  |    displayChars|
    private static void appendLine(byte[] bytes, int firstBlockStart, StringBuilder out) {
        int firstBlockEnd = Math.min(bytes.length, firstBlockStart + 8);
        int secondBlockEnd = Math.min(bytes.length, firstBlockStart + 16);

        appendOffset(firstBlockStart, out);
        out.append(' ');
        out.append(' ');

        appendHexBytes(bytes, firstBlockStart, firstBlockEnd, out);
        out.append(' ');

        appendHexBytes(bytes, firstBlockStart + 8, secondBlockEnd, out);
        padMissingBytes(firstBlockStart, secondBlockEnd, out);
        out.append(' ');

        out.append('|');
        appendDisplayChars(bytes, firstBlockStart, secondBlockEnd, out);
        out.append('|');
        out.append('\n');
    }

    // visible for testing
    static void appendOffset(int offset, StringBuilder out) {
        appendHexChars((byte) ((offset & 0xFF000000) >> 24), out);
        appendHexChars((byte) ((offset & 0x00FF0000) >> 16), out);
        appendHexChars((byte) ((offset & 0x0000FF00) >> 8), out);
        appendHexChars((byte) ((offset & 0x000000FF) >> 0), out);
    }

    private static void appendHexBytes(byte[] bytes, int offset, int limit, StringBuilder out) {
        for (int i = offset; i < limit; i++) {
            appendHexChars(bytes[i], out);
            out.append(' ');
        }
    }

    private static void appendHexChars(byte b, StringBuilder out) {
        out.append(HEX[(b >> 4) & 0x0F]); // 4 high bits
        out.append(HEX[b & 0x0F]); // 4 low bits
    }

    private static void padMissingBytes(int firstByte, int lastByte, StringBuilder out) {
        int charsPerByte = 3;
        int maxBytesPerLine = 16;
        int bytesWritten = lastByte - firstByte;

        int charsMissing = charsPerByte * (maxBytesPerLine - bytesWritten);
        for (int i = 0; i < charsMissing; i++) {
            out.append(' ');
        }
    }

    private static void appendDisplayChars(byte[] bytes, int offset, int blockEnd, StringBuilder out) {
        for (int i = offset; i < blockEnd; i++) {
            appendDisplayChar(bytes[i], out);
        }
    }

    private static void appendDisplayChar(byte b, StringBuilder out) {
        switch (b) {
        case 0x20:
            out.append("\u2423"); // SPACE ␣
            break;
        case 0x09:
            out.append('\u2192'); // TAB →
            break;
        case 0x0a:
            out.append('\u00b6'); // LF ¶
            break;
        case 0x0d:
            out.append('\u00a4'); // CR ¤
            break;
        default:
            out.append((32 <= b && b <= 126) ? (char) b : NON_PRINTABLE); // ' ' to '~', non-printable is WHITE SQUARE □
        }

    }

}