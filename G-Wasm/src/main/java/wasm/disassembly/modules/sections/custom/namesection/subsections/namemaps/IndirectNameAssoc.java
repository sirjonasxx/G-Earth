package wasm.disassembly.modules.sections.custom.namesection.subsections.namemaps;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.conventions.Creator;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class IndirectNameAssoc<Idx extends WASMOpCode, InnerIdx extends WASMOpCode> extends WASMOpCode {

    private Idx idx;
    private NameMap<InnerIdx> nameMap;

    public IndirectNameAssoc(BufferedInputStream in, Module module, Creator<Idx> creator, Creator<InnerIdx> innerCreator) throws IOException, InvalidOpCodeException {
        idx = creator.create(in, module);
        nameMap = new NameMap<>(in, module, innerCreator);
    }

    public IndirectNameAssoc(Idx idx, NameMap<InnerIdx> nameMap) {
        this.idx = idx;
        this.nameMap = nameMap;
    }

    public Idx getIdx() {
        return idx;
    }

    public void setIdx(Idx idx) {
        this.idx = idx;
    }

    public NameMap<InnerIdx> getNameMap() {
        return nameMap;
    }

    public void setNameMap(NameMap<InnerIdx> nameMap) {
        this.nameMap = nameMap;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        idx.assemble(out);
        nameMap.assemble(out);
    }
}
