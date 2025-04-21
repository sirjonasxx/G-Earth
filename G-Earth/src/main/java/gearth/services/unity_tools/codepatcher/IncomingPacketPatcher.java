package gearth.services.unity_tools.codepatcher;

import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.instructions.control.IfElseInstr;
import wasm.disassembly.instructions.memory.MemInstr;
import wasm.disassembly.modules.sections.code.Func;
import wasm.disassembly.modules.sections.code.Locals;
import wasm.disassembly.types.FuncType;
import wasm.disassembly.types.ResultType;
import wasm.disassembly.types.ValType;
import wasm.misc.StreamReplacement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Locate the class of this method with the string literals:
 * - "StartTLS"
 * - "InitializeCrypto should be called only for TCPNetworkConnection of type Habbo"
 */
public class IncomingPacketPatcher extends StreamReplacement {
    @Override
    public FuncType getFuncType() {
        return new FuncType(
                new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32, ValType.I32, ValType.I32)),
                new ResultType(Collections.emptyList())
        );
    }

    @Override
    public ReplacementType getReplacementType() {
        return ReplacementType.HOOK_COPYEXPORT;
    }

    @Override
    public String getImportName() {
        return "g_incoming_packet";
    }

    @Override
    public String getExportName() {
        return "_gearth_incoming_copy";
    }

    @Override
    public boolean codeMatches(int id, Func code) {
        if (!(code.getLocalss().equals(Collections.singletonList(new Locals(5, ValType.I32))))) {
            return false;
        }

        final List<Instr> expression = code.getExpression().getInstructions();
        final List<InstrType> expectedExpr = Arrays.asList(
                InstrType.I32_CONST,
                InstrType.I32_LOAD8_U,
                InstrType.I32_EQZ,
                InstrType.IF,
                InstrType.I32_CONST,
                InstrType.I32_LOAD,
                InstrType.LOCAL_SET,
                InstrType.BLOCK,
                InstrType.LOCAL_SET,
                InstrType.LOCAL_GET,
                InstrType.LOCAL_GET,
                InstrType.LOCAL_GET,
                InstrType.LOCAL_GET,
                InstrType.LOCAL_GET,
                InstrType.I32_LOAD);

        if (!codeStartsWith(expression, expectedExpr)) {
            return false;
        }

        // Sanity check.
        final MemInstr loadInstr = (MemInstr) expression.get(5);

        if (loadInstr.getMemArg().getAlign() != 2 || loadInstr.getMemArg().getOffset() != 0) {
            return false;
        }

        // Count amount of CALL in the if block.
        final long callCount = ((IfElseInstr) expression.get(3)).getIfInstructions()
                .stream()
                .filter(instr -> instr.getInstrType() == InstrType.CALL)
                .count();

        if (callCount != 3) {
            return false;
        }

        return true;
    }
}
