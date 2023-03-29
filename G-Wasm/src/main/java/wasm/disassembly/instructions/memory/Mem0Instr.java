package wasm.disassembly.instructions.memory;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Mem0Instr extends Instr {
    public Mem0Instr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);
        if (in.read() != 0x00) {
            throw new InvalidOpCodeException("Unexpected non-zero byte");
        }
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException {
        out.write(0x00);
    }
}
