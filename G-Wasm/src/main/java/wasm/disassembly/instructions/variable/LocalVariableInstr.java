package wasm.disassembly.instructions.variable;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.LocalIdx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LocalVariableInstr extends Instr {

    private LocalIdx localIdx;

    public LocalVariableInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);
        localIdx = new LocalIdx(in, module);
    }

    public LocalVariableInstr(InstrType instrType, LocalIdx localIdx) throws IOException {
        super(instrType);
        this.localIdx = localIdx;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        localIdx.assemble(out);
    }

    public LocalIdx getLocalIdx() {
        return localIdx;
    }

    public void setLocalIdx(LocalIdx localIdx) {
        this.localIdx = localIdx;
    }
}
