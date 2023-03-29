package wasm.disassembly.modules.indices;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MemIdx extends WASMOpCode {

    private long x;

    public MemIdx(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        x = WUnsignedInt.read(in, 32);
    }

    public MemIdx(long x) {
        this.x = x;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        WUnsignedInt.write(x, out, 32);
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }
}
