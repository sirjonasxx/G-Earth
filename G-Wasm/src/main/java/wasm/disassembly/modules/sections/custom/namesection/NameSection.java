package wasm.disassembly.modules.sections.custom.namesection;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.sections.custom.CustomSection;
import wasm.disassembly.modules.sections.custom.namesection.subsections.FunctionNamesSubSection;
import wasm.disassembly.modules.sections.custom.namesection.subsections.LocalNamesSubSection;
import wasm.disassembly.modules.sections.custom.namesection.subsections.ModuleNameSubSection;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NameSection extends CustomSection {

    private ModuleNameSubSection moduleName;
    private FunctionNamesSubSection functionNames;
    private LocalNamesSubSection localNames;

    public NameSection(BufferedInputStream in, Module module, long size) throws IOException, InvalidOpCodeException {
        super(module, size, "name");

        moduleName = isNextSection(in, 0) ? new ModuleNameSubSection(in, module) : null;
        functionNames = isNextSection(in, 1) ? new FunctionNamesSubSection(in, module) : null;
        localNames = isNextSection(in, 2) ? new LocalNamesSubSection(in, module) : null;
    }

    public NameSection(Module module, long size, String name, ModuleNameSubSection moduleName, FunctionNamesSubSection functionNames, LocalNamesSubSection localNames) throws IOException, InvalidOpCodeException {
        super(module, size, name);
        this.moduleName = moduleName;
        this.functionNames = functionNames;
        this.localNames = localNames;
    }

    public ModuleNameSubSection getModuleName() {
        return moduleName;
    }

    public void setModuleName(ModuleNameSubSection moduleName) {
        this.moduleName = moduleName;
    }

    public FunctionNamesSubSection getFunctionNames() {
        return functionNames;
    }

    public void setFunctionNames(FunctionNamesSubSection functionNames) {
        this.functionNames = functionNames;
    }

    public LocalNamesSubSection getLocalNames() {
        return localNames;
    }

    public void setLocalNames(LocalNamesSubSection localNames) {
        this.localNames = localNames;
    }

    @Override
    protected void assemble3(OutputStream out) throws IOException, InvalidOpCodeException {
        if (moduleName != null) moduleName.assemble(out);
        if (functionNames != null) functionNames.assemble(out);
        if (localNames != null) localNames.assemble(out);
    }

    private boolean isNextSection(BufferedInputStream in, int id) throws IOException {
        in.mark(1);
        if (in.read() == id) {
            return true;
        }
        in.reset();
        return false;
    }
}
