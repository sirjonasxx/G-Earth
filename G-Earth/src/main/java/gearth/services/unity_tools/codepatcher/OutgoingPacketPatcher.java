package gearth.services.unity_tools.codepatcher;

import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.sections.code.Func;
import wasm.disassembly.types.FuncType;
import wasm.disassembly.types.ResultType;
import wasm.disassembly.types.ValType;
import wasm.misc.CodeCompare;
import wasm.misc.StreamReplacement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OutgoingPacketPatcher implements StreamReplacement {
    @Override
    public FuncType getFuncType() {
        return new FuncType(new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32)),
                new ResultType(Collections.emptyList()));
    }

    @Override
    public ReplacementType getReplacementType() {
        return ReplacementType.HOOKCOPYEXPORT;
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
    public CodeCompare getCodeCompare() {
        return code -> {
            if (code.getLocalss().size() != 0) return false;
            List<Instr> expression = code.getExpression().getInstructions();
            if (expression.get(0).getInstrType() != InstrType.LOCAL_GET) return false;
            if (expression.get(1).getInstrType() != InstrType.LOCAL_GET) return false;
            if (expression.get(2).getInstrType() != InstrType.LOCAL_GET) return false;
            if (expression.get(3).getInstrType() != InstrType.I32_LOAD) return false;
            if (expression.get(4).getInstrType() != InstrType.I32_CONST) return false;
            if (expression.get(5).getInstrType() != InstrType.CALL) return false;

            return true;
        };
    }
}
