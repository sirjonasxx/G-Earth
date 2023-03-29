package wasm.disassembly.modules.sections.start;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.sections.Section;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StartSection extends Section {

    public static final int START_SECTION_ID = 8;

    private Start start;

    public StartSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, START_SECTION_ID);
        start = new Start(in, module);
    }

    public StartSection(Module module, Start start) {
        super(module, START_SECTION_ID);
        this.start = start;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        start.assemble(out);
    }


    public Start getStart() {
        return start;
    }

    public void setStart(Start start) {
        this.start = start;
    }
}
