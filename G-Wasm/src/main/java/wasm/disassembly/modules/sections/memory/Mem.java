package wasm.disassembly.modules.sections.memory;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.types.MemType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Mem extends WASMOpCode {

    private MemType memType;

    public Mem(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        memType = new MemType(in, module);
    }

    public Mem(MemType memType) {
        this.memType = memType;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        memType.assemble(out);
    }


    public MemType getMemType() {
        return memType;
    }

    public void setMemType(MemType memType) {
        this.memType = memType;
    }
}
