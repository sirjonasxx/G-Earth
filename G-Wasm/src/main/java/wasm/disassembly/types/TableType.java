package wasm.disassembly.types;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TableType extends WASMOpCode {

    private ElemType elemType;
    private Limits limits;

    public TableType(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        elemType = ElemType.from_val(in.read());
        if (elemType == null) {
            throw new InvalidOpCodeException("No such element type");
        }
        limits = new Limits(in, module);
    }

    public TableType(ElemType elemType, Limits limits) {
        this.elemType = elemType;
        this.limits = limits;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(elemType.val);
        limits.assemble(out);
    }

    public ElemType getElemType() {
        return elemType;
    }

    public void setElemType(ElemType elemType) {
        this.elemType = elemType;
    }

    public Limits getLimits() {
        return limits;
    }

    public void setLimits(Limits limits) {
        this.limits = limits;
    }
}
