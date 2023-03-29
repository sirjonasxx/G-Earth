package wasm.disassembly.types;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class GlobalType extends WASMOpCode {

    private ValType valType;
    private Mutability mutability;

    public GlobalType(BufferedInputStream in, Module module) throws IOException {
        valType = ValType.from_val(in.read());
        mutability = Mutability.from_val(in.read());
    }

    public GlobalType(ValType valType, Mutability mutability) {
        this.valType = valType;
        this.mutability = mutability;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(valType.val);
        out.write(mutability.val);
    }

    public ValType getValType() {
        return valType;
    }

    public void setValType(ValType valType) {
        this.valType = valType;
    }

    public Mutability getMutability() {
        return mutability;
    }

    public void setMutability(Mutability mutability) {
        this.mutability = mutability;
    }
}
