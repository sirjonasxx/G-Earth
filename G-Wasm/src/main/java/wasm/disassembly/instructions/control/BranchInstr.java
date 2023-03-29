package wasm.disassembly.instructions.control;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.LabelIdx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BranchInstr extends Instr {

    private LabelIdx labelIdx;

    public BranchInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);
        labelIdx = new LabelIdx(in, module);
    }

    public BranchInstr(InstrType instrType, LabelIdx labelIdx) throws IOException {
        super(instrType);
        this.labelIdx = labelIdx;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        labelIdx.assemble(out);
    }

    public LabelIdx getLabelIdx() {
        return labelIdx;
    }

    public void setLabelIdx(LabelIdx labelIdx) {
        this.labelIdx = labelIdx;
    }
}
