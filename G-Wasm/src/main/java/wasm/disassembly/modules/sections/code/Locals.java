package wasm.disassembly.modules.sections.code;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.types.ValType;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Locals extends WASMOpCode {

    private long amount;
    private ValType valType;

    public Locals(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        amount = WUnsignedInt.read(in, 32);
        valType = ValType.from_val(in.read());
    }

    public Locals(long amount, ValType valType) {
        this.amount = amount;
        this.valType = valType;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        WUnsignedInt.write(amount, out, 32);
        out.write(valType.val);
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public ValType getValType() {
        return valType;
    }

    public void setValType(ValType valType) {
        this.valType = valType;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Locals)) return false;
        Locals other = (Locals) obj;
        return amount == other.amount && valType.val == other.valType.val;
    }
}
