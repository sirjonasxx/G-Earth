package wasm.disassembly.instructions.numeric;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TruncSatInstr extends Instr {

    private long type;

    public TruncSatInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);
        type = WUnsignedInt.read(in, 32);
        if (type < 0 || type > 7) {
            throw new InvalidOpCodeException("Invalid opcode");
        }
    }

    public TruncSatInstr(InstrType instrType, long type) throws IOException {
        super(instrType);
        this.type = type;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        WUnsignedInt.write(type, out, 32);
    }

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }
}
