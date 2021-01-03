package gearth.services.unity_tools.codepatcher;

import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.modules.sections.code.Func;
import wasm.disassembly.modules.sections.code.Locals;
import wasm.disassembly.types.FuncType;
import wasm.disassembly.types.ResultType;
import wasm.disassembly.types.ValType;
import wasm.misc.CodeCompare;
import wasm.misc.StreamReplacement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IncomingPacketPatcher implements StreamReplacement {
    @Override
    public FuncType getFuncType() {
        return new FuncType(new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32, ValType.I32, ValType.I32)),
                new ResultType(Collections.emptyList())
        );
    }

    @Override
    public ReplacementType getReplacementType() {
        return ReplacementType.HOOKCOPYEXPORT;
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
    public CodeCompare getCodeCompare() {
        return code -> {
            if (!(code.getLocalss().equals(Collections.singletonList(new Locals(1, ValType.I32)))))
                return false;

            List<InstrType> expectedExpr = Arrays.asList(InstrType.I32_CONST, InstrType.I32_LOAD8_S,
                    InstrType.I32_EQZ, InstrType.IF, InstrType.LOCAL_GET, InstrType.I32_LOAD, InstrType.LOCAL_TEE,
                    InstrType.IF);

            if (code.getExpression().getInstructions().size() != expectedExpr.size()) return false;

            for (int j = 0; j < code.getExpression().getInstructions().size(); j++) {
                Instr instr = code.getExpression().getInstructions().get(j);
                if (instr.getInstrType() != expectedExpr.get(j)) return false;
            }

            return true;
        };
    }
}
