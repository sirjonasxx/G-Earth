package wasm.disassembly.instructions;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Expression extends WASMOpCode {

    private List<Instr> instructions;

    public Expression(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        instructions = new ArrayList<>();
        InstrType type;
        while ((type = InstrFactory.disassembleType(in)) != InstrType.END) {
            instructions.add(InstrFactory.disassemble(in, type, module));
        }
    }

    public Expression(List<Instr> instructions) {
        this.instructions = instructions;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        for(Instr instr : instructions) {
            instr.assemble(out);
        }
        out.write(InstrType.END.val);
    }

    public List<Instr> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<Instr> instructions) {
        this.instructions = instructions;
    }
}
