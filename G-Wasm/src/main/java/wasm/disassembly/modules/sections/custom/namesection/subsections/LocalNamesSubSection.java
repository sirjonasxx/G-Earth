package wasm.disassembly.modules.sections.custom.namesection.subsections;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Creator;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.indices.LocalIdx;
import wasm.disassembly.modules.sections.custom.namesection.subsections.namemaps.IndirectNameMap;
import wasm.disassembly.modules.sections.custom.namesection.subsections.namemaps.NameMap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LocalNamesSubSection extends SubSection {

    public static final int LOCAL_NAMES_SUBSECTION_ID = 2;
    private IndirectNameMap<FuncIdx, LocalIdx> indirectNameMap;

    public LocalNamesSubSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, LOCAL_NAMES_SUBSECTION_ID);
        indirectNameMap = new IndirectNameMap<>(in, module, FuncIdx::new, LocalIdx::new);
    }

    public LocalNamesSubSection(IndirectNameMap<FuncIdx, LocalIdx> indirectNameMap) {
        super(LOCAL_NAMES_SUBSECTION_ID);
        this.indirectNameMap = indirectNameMap;
    }

    public IndirectNameMap<FuncIdx, LocalIdx> getIndirectNameMap() {
        return indirectNameMap;
    }

    public void setIndirectNameMap(IndirectNameMap<FuncIdx, LocalIdx> indirectNameMap) {
        this.indirectNameMap = indirectNameMap;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        indirectNameMap.assemble(out);
    }
}
