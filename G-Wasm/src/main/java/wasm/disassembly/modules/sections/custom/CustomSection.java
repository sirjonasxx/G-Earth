package wasm.disassembly.modules.sections.custom;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.sections.Section;
import wasm.disassembly.values.WName;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class CustomSection extends Section {

    public static final int CUSTOM_SECTION_ID = 0;

    private String name;

    public CustomSection(Module module, long size, String name) throws IOException, InvalidOpCodeException {
        super(module, CUSTOM_SECTION_ID, size);
        this.name = name;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        WName.write(name, out);
        assemble3(out);
    }

    protected abstract void assemble3(OutputStream out) throws IOException, InvalidOpCodeException;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
