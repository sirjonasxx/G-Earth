package wasm.misc;

import wasm.disassembly.modules.sections.code.Func;
import wasm.disassembly.types.FuncType;

public interface StreamReplacement {


    enum ReplacementType {
        HOOK,
        HOOKCOPYEXPORT
    }

    FuncType getFuncType();
    ReplacementType getReplacementType();
    String getImportName();
    String getExportName();
    boolean codeMatches(Func code);
}
