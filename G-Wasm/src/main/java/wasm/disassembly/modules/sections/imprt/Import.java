package wasm.disassembly.modules.sections.imprt;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WName;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Import extends WASMOpCode {

    private String module;
    private String name;
    private ImportDesc importDescription;

    public Import(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        this.module = WName.read(in);
        name = WName.read(in);
        importDescription = new ImportDesc(in, module);
    }

    public Import(String module, String name, ImportDesc importDescription) {
        this.module = module;
        this.name = name;
        this.importDescription = importDescription;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        WName.write(module, out);
        WName.write(name, out);
        importDescription.assemble(out);
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ImportDesc getImportDescription() {
        return importDescription;
    }

    public void setImportDescription(ImportDesc importDescription) {
        this.importDescription = importDescription;
    }
}
