package wasm.disassembly.modules.sections.custom.namesection.subsections.namemaps;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.conventions.Creator;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WName;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NameAssoc<Idx extends WASMOpCode> extends WASMOpCode {

    private Idx idx;
    private String name;

    public NameAssoc(BufferedInputStream in, Module module, Creator<Idx> creator) throws IOException, InvalidOpCodeException {
        idx = creator.create(in, module);
        name = WName.read(in);
    }

    public NameAssoc(Idx idx, String name) {
        this.idx = idx;
        this.name = name;
    }

    public Idx getIdx() {
        return idx;
    }

    public void setIdx(Idx idx) {
        this.idx = idx;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        idx.assemble(out);
        WName.write(name, out);
    }
}
