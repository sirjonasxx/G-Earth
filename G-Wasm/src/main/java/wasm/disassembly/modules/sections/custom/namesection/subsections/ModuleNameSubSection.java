package wasm.disassembly.modules.sections.custom.namesection.subsections;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WName;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ModuleNameSubSection extends SubSection {

    public static final int MODULE_NAME_SUBSECTION_ID = 0;

    private String name;

    public ModuleNameSubSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, MODULE_NAME_SUBSECTION_ID);
        name = WName.read(in);
    }

    public ModuleNameSubSection(String name) {
        super(MODULE_NAME_SUBSECTION_ID);
        this.name = name;
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        WName.write(name, out);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
