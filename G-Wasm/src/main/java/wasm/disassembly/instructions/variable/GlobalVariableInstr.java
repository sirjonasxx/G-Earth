package wasm.disassembly.instructions.variable;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.GlobalIdx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GlobalVariableInstr extends Instr {

    private GlobalIdx globalIdx;

    public GlobalVariableInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);
        globalIdx = new GlobalIdx(in, module);
    }

    public GlobalVariableInstr(InstrType instrType, GlobalIdx globalIdx) throws IOException {
        super(instrType);
        this.globalIdx = globalIdx;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        globalIdx.assemble(out);
    }

    public GlobalIdx getGlobalIdx() {
        return globalIdx;
    }

    public void setGlobalIdx(GlobalIdx globalIdx) {
        this.globalIdx = globalIdx;
    }
}
