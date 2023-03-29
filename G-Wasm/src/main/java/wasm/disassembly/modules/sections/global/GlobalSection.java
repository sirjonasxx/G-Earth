package wasm.disassembly.modules.sections.global;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.sections.Section;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class GlobalSection extends Section {

    public static final int GLOBAL_SECTION_ID = 6;

    private Vector<Global> globals;


    public GlobalSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, GLOBAL_SECTION_ID);
        globals = new Vector<>(in, Global::new, module);
    }

    public GlobalSection(Module module, List<Global> globals) {
        super(module, GLOBAL_SECTION_ID);
        this.globals = new Vector<>(globals);
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        globals.assemble(out);
    }

    public List<Global> getGlobals() {
        return globals.getElements();
    }

    public void setGlobals(List<Global> globals) {
        this.globals = new Vector<>(globals);
    }
}
