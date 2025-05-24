package gearth.protocol.crypto;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Non-standard RC4 algorithm using base64.
 * Thanks to Joni Aromaa for the original implementation.
 * <a href="https://github.com/aromaa/Skylight3/blob/72ec3a07d126de09f6de4251c91001329f77a8a2/src/Skylight.Server/Net/Crypto/RC4Base64.cs">
 *     https://github.com/aromaa/Skylight3/blob/72ec3a07d126de09f6de4251c91001329f77a8a2/src/Skylight.Server/Net/Crypto/RC4Base64.cs
 * </a>
 * Modified by Mikee to 512 byte support.
 */
public class RC4Shockwave implements RC4Cipher {

    private static final byte[] BASE64_ENCODING_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] BASE64_DECODING_MAP = new byte[]{
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
            -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
            -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    };

    private final int[] state;
    private int q;
    private int j;

    public RC4Shockwave(byte[] state, int q, int j) {
        this(convertToIntState(state), q, j);
    }

    public RC4Shockwave(int[] state, int q, int j) {
        if (state.length != 512) {
            throw new IllegalArgumentException(String.format("Int state must be of size 512, was %d", state.length));
        }

        this.q = q;
        this.j = j;
        this.state = state;
    }

    private static int[] convertToIntState(byte[] stateDump) {
        final ByteBuffer buffer = ByteBuffer.wrap(stateDump);
        final int[] state = new int[stateDump.length / 4];

        for (int i = 0; i < state.length; i++) {
            state[i] = buffer.getInt();
        }

        return state;
    }

    @Override
    public byte[] cipher(byte[] data) {
        return cipher(data, 0, data.length);
    }

    @Override
    public byte[] cipher(byte[] data, int offset, int length) {
        final ByteArrayOutputStream result = new ByteArrayOutputStream();

        for (int i = 0; i < length; i += 3) {
            int firstByte = data[offset + i] ^ moveUp();
            int secondByte = data.length > i + 1 ? (data[i + 1] ^ moveUp()) : 0;

            result.write(BASE64_ENCODING_MAP[(firstByte & 0xFC) >> 2]);
            result.write(BASE64_ENCODING_MAP[((firstByte & 0x03) << 4) | ((secondByte & 0xF0) >> 4)]);

            if (data.length > i + 1) {
                int thirdByte = data.length > i + 2 ? (data[i + 2] ^ moveUp()) : 0;

                result.write(BASE64_ENCODING_MAP[((secondByte & 0x0F) << 2) | ((thirdByte & 0xC0) >> 6)]);

                if (data.length > i + 2) {
                    result.write(BASE64_ENCODING_MAP[thirdByte & 0x3F]);
                }
            }
        }

        return result.toByteArray();
    }

    @Override
    public byte[] decipher(byte[] data) {
        return decipher(data, 0, data.length);
    }

    @Override
    public byte[] decipher(byte[] data, int offset, int length) {
        final ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        final ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();

        while (buffer.hasRemaining()) {
            int firstByte = BASE64_DECODING_MAP[buffer.get()];
            int secondByte = BASE64_DECODING_MAP[buffer.get()];

            int byte1a = firstByte << 2;
            int byte1b = (secondByte & 0x30) >> 4;

            resultBuffer.write((byte) ((byte1a | byte1b) ^ moveUp()));

            if (buffer.hasRemaining()) {
                int thirdByte = BASE64_DECODING_MAP[buffer.get()];

                int byte2a = (secondByte & 0x0F) << 4;
                int byte2b = (thirdByte & 0x3C) >> 2;

                resultBuffer.write((byte) ((byte2a | byte2b) ^ moveUp()));

                if (buffer.hasRemaining()) {
                    int fourthByte = BASE64_DECODING_MAP[buffer.get()];

                    int byte3a = (thirdByte & 0x03) << 6;
                    int byte3b = fourthByte & 0x3F;

                    resultBuffer.write((byte) ((byte3a | byte3b) ^ moveUp()));
                }
            }
        }

        return resultBuffer.toByteArray();
    }

    @Override
    public int[] getState() {
        return state;
    }

    @Override
    public int getQ() {
        return q;
    }

    @Override
    public int getJ() {
        return j;
    }

    @Override
    public RC4Shockwave deepCopy() {
        return new RC4Shockwave(Arrays.copyOf(state, state.length), q, j);
    }

    public byte moveUp() {
        q = (q + 1) & 0xff;
        j = ((state[q] & 0xff) + j) & 0xff;

        int tmp = state[q];
        state[q] = state[j];
        state[j] = tmp;

        if ((q & 128) == 128) {
            int x2 = 279 * (q + 67) & 0xff;
            int y2 = (j + (state[x2] & 0xff)) & 0xff;

            tmp = state[x2];
            state[x2] = state[y2];
            state[y2] = tmp;
        }

        int xorIndex = ((state[q] &0xff) + (state[j] & 0xff)) & 0xff;

        return (byte) (state[xorIndex] & 0xff);
    }

    public boolean moveDown() {
        int tmp;

        if ((q & 128) == 128) {
            // Unsupported.
            return false;
        }

        tmp = state[q];
        state[q] = state[j];
        state[j] = tmp;

        j = ((j - (state[q])) % 256) & 0xff;
        q = (q - 1) % 256;

        return true;
    }

    public boolean undoRc4(int length) {
        for (int i = 0; i < length; i++) {
            if (!moveDown()) {
                return false;
            }
        }

        return true;
    }
}