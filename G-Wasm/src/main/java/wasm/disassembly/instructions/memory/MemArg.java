package wasm.disassembly.instructions.memory;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MemArg extends WASMOpCode {

    private long align;
    private long offset;

    public MemArg(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        align = WUnsignedInt.read(in, 32);
        offset = WUnsignedInt.read(in, 32);
    }

    public MemArg(long align, long offset) {
        this.align = align;
        this.offset = offset;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        WUnsignedInt.write(align, out, 32);
        WUnsignedInt.write(offset, out, 32);
    }

    public long getAlign() {
        return align;
    }

    public void setAlign(long align) {
        this.align = align;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
