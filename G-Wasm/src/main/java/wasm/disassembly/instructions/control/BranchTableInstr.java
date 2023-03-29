package wasm.disassembly.instructions.control;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Creator;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.LabelIdx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BranchTableInstr extends Instr {

    private Vector<LabelIdx> table;
    private LabelIdx labelIdx;

    public BranchTableInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);
        table = new Vector<>(in, LabelIdx::new, module);
        labelIdx = new LabelIdx(in, module);
    }

    public BranchTableInstr(InstrType instrType, Vector<LabelIdx> table, LabelIdx labelIdx) throws IOException {
        super(instrType);
        this.table = table;
        this.labelIdx = labelIdx;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        table.assemble(out);
        labelIdx.assemble(out);
    }

    public Vector<LabelIdx> getTable() {
        return table;
    }

    public void setTable(Vector<LabelIdx> table) {
        this.table = table;
    }

    public LabelIdx getLabelIdx() {
        return labelIdx;
    }

    public void setLabelIdx(LabelIdx labelIdx) {
        this.labelIdx = labelIdx;
    }
}
