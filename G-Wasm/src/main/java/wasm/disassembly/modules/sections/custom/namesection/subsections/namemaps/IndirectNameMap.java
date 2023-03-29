package wasm.disassembly.modules.sections.custom.namesection.subsections.namemaps;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.conventions.Creator;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class IndirectNameMap<Idx extends WASMOpCode, InnerIdx extends WASMOpCode> extends WASMOpCode {

    private Vector<IndirectNameAssoc<Idx, InnerIdx>> idxAndMaps;

    public IndirectNameMap(BufferedInputStream in, Module module, Creator<Idx> creator, Creator<InnerIdx> innerCreator) throws IOException, InvalidOpCodeException {
        idxAndMaps = new Vector<>(in, (in1, m) -> new IndirectNameAssoc<>(in1, m, creator, innerCreator), module);
    }

    public IndirectNameMap(Vector<IndirectNameAssoc<Idx, InnerIdx>> idxAndMaps) {
        this.idxAndMaps = idxAndMaps;
    }

    public List<IndirectNameAssoc<Idx, InnerIdx>> getIdxAndMaps() {
        return idxAndMaps.getElements();
    }

    public void setIdxAndMaps(List<IndirectNameAssoc<Idx, InnerIdx>> idxAndMaps) {
        this.idxAndMaps = new Vector<>(idxAndMaps);
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        idxAndMaps.assemble(out);
    }
}
