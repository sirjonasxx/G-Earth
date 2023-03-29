package wasm.disassembly.modules.sections.custom.namesection.subsections;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Creator;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.sections.custom.namesection.subsections.namemaps.NameMap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FunctionNamesSubSection extends SubSection {

    public static final int FUNCTION_NAMES_SUBSECTION_ID = 1;
    private NameMap<FuncIdx> nameMap;

    public FunctionNamesSubSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, FUNCTION_NAMES_SUBSECTION_ID);
        nameMap = new NameMap<>(in, module, FuncIdx::new);
    }

    public FunctionNamesSubSection(NameMap<FuncIdx> nameMap) {
        super(FUNCTION_NAMES_SUBSECTION_ID);
        this.nameMap = nameMap;
    }

    public NameMap<FuncIdx> getNameMap() {
        return nameMap;
    }

    public void setNameMap(NameMap<FuncIdx> nameMap) {
        this.nameMap = nameMap;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        nameMap.assemble(out);
    }
}
