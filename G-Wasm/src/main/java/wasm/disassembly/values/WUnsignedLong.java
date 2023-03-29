package wasm.disassembly.values;

import wasm.disassembly.InvalidOpCodeException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

public class WUnsignedLong {

    // everything with N > 63
    public static BigInteger read(BufferedInputStream in, int N) throws InvalidOpCodeException, IOException, IOException {
        BigInteger result = BigInteger.ZERO;
        long cur;
        int count = 0;
        int limit = N/7 + (N%7 != 0 ? 1 : 0);

        do {
            cur = in.read() & 0xff;
            result = result.or(BigInteger.valueOf(cur & 0x7f).shiftLeft(count * 7));
            count++;
        } while (((cur & 0x80) == 0x80) && count < limit);

        if ((cur & 0x80) == 0x80) {
            throw new InvalidOpCodeException("invalid LEB128 sequence");
        }

        return result;
    }


    public static void write(BigInteger value, OutputStream out, int N) throws InvalidOpCodeException, IOException {
        long remaining = value.shiftRight(7).longValueExact();
        long l_value = -1;
        boolean first = true;

        while (remaining != 0) {
            if (first) {
                out.write((byte) ((value.longValue() & 0x7f) | 0x80));
                first = false;
            }
            else {
                out.write((byte) ((l_value & 0x7f) | 0x80));
            }
            l_value = remaining;
            remaining >>>= 7;
        }
        if (first) {
            out.write((byte) (value.longValue() & 0x7f));
        }
        else {
            out.write((byte) (l_value & 0x7f));
        }
    }


}
