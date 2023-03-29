package wasm.disassembly.values;

import wasm.disassembly.InvalidOpCodeException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WUnsignedInt {

    // everything with N <= 63
    public static long read(BufferedInputStream in, int N) throws InvalidOpCodeException, IOException, IOException {
        if (N >= 64) throw new InvalidOpCodeException("Invalid integer size");

        long result = 0;
        long cur;
        int count = 0;
        int limit = N/7 + (N%7 != 0 ? 1 : 0);

        do {
            cur = in.read() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        } while (((cur & 0x80) == 0x80) && count < limit);

        if ((cur & 0x80) == 0x80) {
            throw new InvalidOpCodeException("invalid LEB128 sequence");
        }

        return result;
    }

    public static void write(long value, OutputStream out, int N) throws InvalidOpCodeException, IOException {
        if (N >= 64) throw new InvalidOpCodeException("Invalid integer size");

        long remaining = value >>> 7;

        while (remaining != 0) {
            out.write((byte)((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }

        out.write((byte)(value & 0x7f));
    }

}
