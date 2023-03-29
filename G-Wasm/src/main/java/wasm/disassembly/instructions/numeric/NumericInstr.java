package wasm.disassembly.instructions.numeric;

import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NumericInstr extends Instr {

    public NumericInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException {
        super(instrType);
    }

    @Override
    protected void assemble2(OutputStream out) {
        // nothing
    }
}
