package wasm.disassembly.instructions;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;

import java.io.IOException;
import java.io.OutputStream;

public abstract class Instr extends WASMOpCode {

    private InstrType instrType;

    public Instr(InstrType instrType) throws IOException {
        this.instrType = instrType;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(instrType.val);
        assemble2(out);
    }

    protected abstract void assemble2(OutputStream out) throws IOException, InvalidOpCodeException;

    public InstrType getInstrType() {
        return instrType;
    }

    public void setInstrType(InstrType instrType) {
        this.instrType = instrType;
    }
}
