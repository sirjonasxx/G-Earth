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

public class NameMap<Idx extends WASMOpCode> extends WASMOpCode {

    private Vector<NameAssoc<Idx>> idxAndNames;

    public NameMap(BufferedInputStream in, Module module, Creator<Idx> creator) throws IOException, InvalidOpCodeException {
        idxAndNames = new Vector<>(in, (in1, m) -> new NameAssoc<>(in1, m, creator), module);
    }

    public NameMap(Vector<NameAssoc<Idx>> idxAndNames) {
        this.idxAndNames = idxAndNames;
    }

    public List<NameAssoc<Idx>> getIdxAndNames() {
        return idxAndNames.getElements();
    }

    public void setIdxAndNames(List<NameAssoc<Idx>> idxAndNames) {
        this.idxAndNames = new Vector<>(idxAndNames);
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        idxAndNames.assemble(out);
    }
}
