package wasm.disassembly.instructions.control;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrFactory;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class IfElseInstr extends Instr {

    private List<Instr> ifInstructions;
    private List<Instr> elseInstructions;

    private BlockType blockType;


    public IfElseInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);

        blockType = new BlockType(in, module);
        ifInstructions = new ArrayList<>(2);
        elseInstructions = null;
        List<Instr> currentBlock = ifInstructions;

        InstrType type;
        while ((type = InstrFactory.disassembleType(in)) != InstrType.END) {
            if (type == InstrType.ELSE) {
                elseInstructions = new ArrayList<>(2);
                currentBlock = elseInstructions;
            }
            else {
                currentBlock.add(InstrFactory.disassemble(in, type, module));
            }
        }
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        blockType.assemble(out);
        for(Instr instr : ifInstructions) {
            instr.assemble(out);
        }
        if (elseInstructions != null) {
            out.write(InstrType.ELSE.val);
            for(Instr instr : elseInstructions) {
                instr.assemble(out);
            }
        }
        out.write(InstrType.END.val);
    }

    public IfElseInstr(InstrType instrType, List<Instr> ifInstructions, List<Instr> elseInstructions, BlockType blockType) throws IOException {
        super(instrType);
        this.ifInstructions = ifInstructions;
        this.elseInstructions = elseInstructions;
        this.blockType = blockType;
    }

    public List<Instr> getIfInstructions() {
        return ifInstructions;
    }

    public void setIfInstructions(List<Instr> ifInstructions) {
        this.ifInstructions = ifInstructions;
    }

    public List<Instr> getElseInstructions() {
        return elseInstructions;
    }

    public void setElseInstructions(List<Instr> elseInstructions) {
        this.elseInstructions = elseInstructions;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public void setBlockType(BlockType blockType) {
        this.blockType = blockType;
    }
}
