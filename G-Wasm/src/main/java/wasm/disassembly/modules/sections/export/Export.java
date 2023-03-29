package wasm.disassembly.modules.sections.export;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WName;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Export extends WASMOpCode {

    private String name;
    private ExportDesc exportDesc;

    public Export(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        name = WName.read(in);
        exportDesc = new ExportDesc(in, module);
    }

    public Export(String name, ExportDesc exportDesc) {
        this.name = name;
        this.exportDesc = exportDesc;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        WName.write(name, out);
        exportDesc.assemble(out);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExportDesc getExportDesc() {
        return exportDesc;
    }

    public void setExportDesc(ExportDesc exportDesc) {
        this.exportDesc = exportDesc;
    }
}
