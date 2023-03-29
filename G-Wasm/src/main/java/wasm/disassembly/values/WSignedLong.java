package wasm.disassembly.values;

import wasm.disassembly.InvalidOpCodeException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WSignedLong {

    // everything with N > 32 && N <= 64
    public static long read(BufferedInputStream in, int N) throws InvalidOpCodeException, IOException {
        long result = 0;
        long cur;
        int count = 0;
        long signBits = -1;

        long limit = N/7 + (N%7 != 0 ? 1 : 0);

        do {
            cur = (in.read()) & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            signBits <<= 7;
            count++;
        } while (((cur & 0x80) == 0x80) && count < limit);

        if ((cur & 0x80) == 0x80) {
            throw new InvalidOpCodeException("invalid LEB128 sequence");
        }

        // Sign extend if appropriate
        if (((signBits >> 1) & result) != 0 ) {
            result |= signBits;
        }

        return result;
    }

    public static void write(long value, OutputStream out, int N) throws InvalidOpCodeException, IOException {
        long remaining = value >> 7;
        boolean hasMore = true;
        long end = ((value & Long.MIN_VALUE) == 0) ? 0 : -1;

        while (hasMore) {
            hasMore = (remaining != end)
                    || ((remaining & 1) != ((value >> 6) & 1));

            out.write((byte)((value & 0x7f) | (hasMore ? 0x80 : 0)));
            value = remaining;
            remaining >>= 7;
        }
    }

}
