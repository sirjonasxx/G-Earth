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

public class BlockInstr extends Instr {

    private List<Instr> blockInstructions;
    private BlockType blockType;

    public BlockInstr(BufferedInputStream in, InstrType instrType, Module module) throws IOException, InvalidOpCodeException {
        super(instrType);

        blockType = new BlockType(in, module);

        blockInstructions = new ArrayList<>(4);
        InstrType type;
        while ((type = InstrFactory.disassembleType(in)) != InstrType.END) {
            blockInstructions.add(InstrFactory.disassemble(in, type, module));
        }
    }

    public BlockInstr(InstrType instrType, List<Instr> blockInstructions, BlockType blockType) throws IOException {
        super(instrType);
        this.blockInstructions = blockInstructions;
        this.blockType = blockType;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        blockType.assemble(out);
        for(Instr instr : blockInstructions) {
            instr.assemble(out);
        }
        out.write(InstrType.END.val);
    }

    public List<Instr> getBlockInstructions() {
        return blockInstructions;
    }

    public void setBlockInstructions(List<Instr> blockInstructions) {
        this.blockInstructions = blockInstructions;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public void setBlockType(BlockType blockType) {
        this.blockType = blockType;
    }
}
