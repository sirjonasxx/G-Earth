package wasm.disassembly.instructions.memory;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MemInstr extends Instr {

    private MemArg memArg;

    public MemInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);
        memArg = new MemArg(in, module);
    }

    public MemInstr(InstrType instrType, MemArg memArg) throws IOException {
        super(instrType);
        this.memArg = memArg;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        memArg.assemble(out);
    }

    public MemArg getMemArg() {
        return memArg;
    }

    public void setMemArg(MemArg memArg) {
        this.memArg = memArg;
    }
}
