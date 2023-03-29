package wasm.disassembly.types;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Limits extends WASMOpCode {

    private long min;
    private long max;

    public Limits(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        int flag = in.read();
        if (flag == 0x00) {
            min = WUnsignedInt.read(in, 32);
            max = -1;
        }
        else if (flag == 0x01) {
            min = WUnsignedInt.read(in, 32);
            max = WUnsignedInt.read(in, 32);
        }
        else throw new InvalidOpCodeException("Function types must be encoded with 0x00 or 0x01");
    }

    public Limits(long min, long max) {
        this.min = min;
        this.max = max;
    }

    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(max == -1 ? 0x00 : 0x01);
        WUnsignedInt.write(min, out, 32);
        if (max != -1) {
            WUnsignedInt.write(max, out, 32);
        }
    }

    public long getMax() {
        return max;
    }

    public long getMin() {
        return min;
    }

    public boolean hasMax() {
        return max != -1;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public void setMax(long max) {
        this.max = max;
    }
}
