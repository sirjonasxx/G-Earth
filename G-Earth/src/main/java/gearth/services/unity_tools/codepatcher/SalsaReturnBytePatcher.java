package gearth.services.unity_tools.codepatcher;

import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.instructions.control.BlockInstr;
import wasm.disassembly.modules.sections.code.Func;
import wasm.disassembly.types.FuncType;
import wasm.disassembly.types.ResultType;
import wasm.disassembly.types.ValType;
import wasm.misc.StreamReplacement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Namespace    Org.BouncyCastle.Crypto.Engines
 * Class        Salsa20Engine
 * Method       ReturnByte(byte input)
 * Locate function with StringLiteral "2^70 byte limit per IV; Change IV".
 */
public class SalsaReturnBytePatcher extends StreamReplacement {
    @Override
    public FuncType getFuncType() {
        return new FuncType(
                new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32)),
                new ResultType(Collections.singletonList(ValType.I32)));
    }

    @Override
    public ReplacementType getReplacementType() {
        return ReplacementType.HOOK_COPYEXPORT;
    }

    @Override
    public String getImportName() {
        return "g_chacha_returnbyte";
    }

    @Override
    public String getExportName() {
        return "_gearth_returnbyte_copy";
    }

    @Override
    public boolean codeMatches(int id, Func code) {
        // Check locals.
        if (code.getLocalss().size() != 1) {
            return false;
        }

        if (code.getLocalss().get(0).getAmount() != 1 || code.getLocalss().get(0).getValType() != ValType.I32) {
            return false;
        }

        // Check function size.
        final List<Instr> instrs = code.getExpression().getInstructions();

        if (instrs.size() != 22) {
            return false;
        }

        // Instruction 7 is a block.
        final Instr instrEight = instrs.get(8);

        if (instrEight.getInstrType() != InstrType.BLOCK) {
            return false;
        }

        final BlockInstr block = (BlockInstr) instrEight;
        final List<Instr> blockInstr = block.getBlockInstructions();

        if (blockInstr.get(blockInstr.size() - 2).getInstrType() != InstrType.I32_XOR) {
            return false;
        }

        return true;
    }
}
