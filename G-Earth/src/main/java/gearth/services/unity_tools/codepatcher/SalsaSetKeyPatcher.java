package gearth.services.unity_tools.codepatcher;

import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
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
 * Method       SetKey(byte[] keyBytes, byte[] ivBytes)
 * Locate function with StringLiteral "requires 128 bit or 256 bit key".
 */
public class SalsaSetKeyPatcher extends StreamReplacement {

    @Override
    public FuncType getFuncType() {
        return new FuncType(
                new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32, ValType.I32)),
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
    public boolean codeMatches(int id, Func code) {
        if (!code.getLocalss().isEmpty()) {
            return false;
        }

        final List<Instr> expression = code.getExpression().getInstructions();
        final List<InstrType> expectedExpr = Arrays.asList(
                InstrType.LOCAL_GET,
                InstrType.IF,
                InstrType.LOCAL_GET,
                InstrType.I32_CONST,
                InstrType.LOCAL_GET,
                InstrType.I32_LOAD,
                InstrType.I32_CONST,
                InstrType.I32_CONST,
                InstrType.I32_CONST,
                InstrType.CALL);

        if (!codeEquals(expression, expectedExpr)) {
            return false;
        }

        return true;
    }
}
