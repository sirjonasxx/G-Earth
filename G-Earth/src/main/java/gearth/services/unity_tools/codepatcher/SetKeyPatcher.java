package gearth.services.unity_tools.codepatcher;

import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.instructions.numeric.NumericI32ConstInstr;
import wasm.disassembly.instructions.variable.LocalVariableInstr;
import wasm.disassembly.modules.sections.code.Func;
import wasm.disassembly.modules.sections.code.Locals;
import wasm.disassembly.types.FuncType;
import wasm.disassembly.types.ResultType;
import wasm.disassembly.types.ValType;
import wasm.misc.StreamReplacement;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SetKeyPatcher implements StreamReplacement {

    @Override
    public FuncType getFuncType() {
        return new FuncType(new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32, ValType.I32)),
                new ResultType(Collections.emptyList()));
    }

    @Override
    public ReplacementType getReplacementType() {
        return ReplacementType.HOOK;
    }

    @Override
    public String getImportName() {
        return "g_chacha_setkey";
    }

    @Override
    public String getExportName() {
        return null;
    }

    @Override
    public boolean codeMatches(Func code) {
        if (!(code.getLocalss().equals(Collections.singletonList(new Locals(1, ValType.I32)))))
            return false;
        List<Instr> expression = code.getExpression().getInstructions();
        List<InstrType> expectedExpr = Arrays.asList(InstrType.BLOCK, InstrType.LOCAL_GET,
                InstrType.I32_CONST, InstrType.LOCAL_GET, InstrType.I32_LOAD, InstrType.I32_CONST, InstrType.I32_CONST,
                InstrType.I32_CONST, InstrType.CALL );

        if (expression.size() != expectedExpr.size()) return false;

        for (int j = 0; j < expression.size(); j++) {
            Instr instr = expression.get(j);
            if (instr.getInstrType() != expectedExpr.get(j)) return false;
        }

//        if (((NumericI32ConstInstr)(expression.get(5))).getConstValue() != 14) return false;

        return true;
    }
}
