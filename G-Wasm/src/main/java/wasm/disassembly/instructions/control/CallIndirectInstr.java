package wasm.disassembly.instructions.control;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.indices.TypeIdx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CallIndirectInstr extends Instr {

    private TypeIdx typeIdx;

    public CallIndirectInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);
        typeIdx = new TypeIdx(in, module);
        if (in.read() != 0x00) {
            throw new InvalidOpCodeException("Unexpected non-zero byte");
        }
    }

    public CallIndirectInstr(InstrType instrType, TypeIdx typeIdx) throws IOException {
        super(instrType);
        this.typeIdx = typeIdx;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        typeIdx.assemble(out);
        out.write(0x00);
    }

    public TypeIdx getTypeIdx() {
        return typeIdx;
    }

    public void setTypeIdx(TypeIdx typeIdx) {
        this.typeIdx = typeIdx;
    }
}
