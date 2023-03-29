package wasm.disassembly.instructions.numeric;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WFloat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NumericF32ConstInstr extends Instr {

    private float constValue;

    public NumericF32ConstInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);
        constValue = WFloat.read(in);
    }

    public NumericF32ConstInstr(float constValue) throws IOException {
        super(InstrType.F32_CONST);
        this.constValue = constValue;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException {
        WFloat.write(constValue, out);
    }

    public float getConstValue() {
        return constValue;
    }

    public void setConstValue(float constValue) {
        this.constValue = constValue;
    }
}
