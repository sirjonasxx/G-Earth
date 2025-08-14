package gearth.app.services.unity_tools.codepatcher;

import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.instructions.memory.MemInstr;
import wasm.disassembly.instructions.variable.LocalVariableInstr;
import wasm.disassembly.modules.sections.code.Func;
import wasm.disassembly.types.FuncType;
import wasm.disassembly.types.ResultType;
import wasm.disassembly.types.ValType;
import wasm.misc.StreamReplacement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OutgoingPacketPatcher extends StreamReplacement {

    @Override
    public FuncType getFuncType() {
        return new FuncType(
                new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32)),
                new ResultType(Collections.emptyList()));
    }

    @Override
    public ReplacementType getReplacementType() {
        return ReplacementType.HOOK_COPYEXPORT;
    }

    @Override
    public String getImportName() {
        return "g_outgoing_packet";
    }

    @Override
    public String getExportName() {
        return "_gearth_outgoing_copy";
    }

    @Override
    public boolean codeMatches(int id, Func code) {
        if (!code.getLocalss().isEmpty()) {
            return false;
        }

        final List<Instr> expression = code.getExpression().getInstructions();

        if (expression.size() != 3) return false;
        if (expression.get(0).getInstrType() != InstrType.LOCAL_GET) return false;
        if (expression.get(1).getInstrType() != InstrType.I32_LOAD8_U) return false;
        if (expression.get(2).getInstrType() != InstrType.IF) return false;

        // Check local variable index
        if (((LocalVariableInstr) (expression.get(0))).getLocalIdx().getX() != 0) return false;

        // Check load align and offset.
        final MemInstr loadInstr = (MemInstr) expression.get(1);
        if (loadInstr.getMemArg().getAlign() != 0 || loadInstr.getMemArg().getOffset() != 30) return false;

        return true;
    }
}
