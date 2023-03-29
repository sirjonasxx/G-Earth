package wasm.disassembly.modules.indices;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FuncIdx extends WASMOpCode {

    private long x;


//    public int ref; // debugging purpose

    public FuncIdx(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        x = WUnsignedInt.read(in, 32)
                + module.streamReplacements.size();


//        ref = CodeSection.currentI; // debugging purpose
    }

    public FuncIdx(long x, Module module) {
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

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof FuncIdx && ((FuncIdx)obj).getX() == x);
    }
}
