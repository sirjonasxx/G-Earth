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

public class ReturnBytePatcher implements StreamReplacement {
    @Override
    public FuncType getFuncType() {
        return new FuncType(new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32)),
                new ResultType(Collections.singletonList(ValType.I32)));
    }

    @Override
    public ReplacementType getReplacementType() {
        return ReplacementType.HOOKCOPYEXPORT;
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
    public CodeCompare getCodeCompare() {
        return code -> {
            if (code.getLocalss().size() != 0) return false;
            if (code.getExpression().getInstructions().size() != 30) return false;
            List<Instr> expr = code.getExpression().getInstructions();
            if (expr.get(expr.size() - 1).getInstrType() != InstrType.I32_XOR) return false;
            return true;
        };
    }
}
