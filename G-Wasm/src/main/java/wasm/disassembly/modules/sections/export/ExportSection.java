package wasm.disassembly.modules.sections.export;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.sections.Section;
import wasm.misc.StreamReplacement;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ExportSection extends Section {

    public static final int EXPORT_SECTION_ID = 7;

    private Vector<Export> exports;

    public ExportSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, EXPORT_SECTION_ID);
        exports = new Vector<>(in, Export::new, module);
    }

    public ExportSection(Module module, List<Export> exports) {
        super(module, EXPORT_SECTION_ID);
        this.exports = new Vector<>(exports);
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        exports.assemble(out);
    }

    public List<Export> getExports() {
        return exports.getElements();
    }

    public void setExports(List<Export> exports) {
        this.exports = new Vector<>(exports);
    }


    public void addShittyExports(Module module) {
        int count = 0;
        for (int i = 0; i < module.streamReplacements.size(); i++) {
            StreamReplacement.ReplacementType actionTaken = module.streamReplacements.get(i).getReplacementType();
            if (actionTaken == StreamReplacement.ReplacementType.HOOKCOPYEXPORT) {
                getExports().add(new Export(module.streamReplacements.get(i).getExportName(), new ExportDesc(new FuncIdx(
                        module.getCodeSection().length + module.getImportSection().getTotalFuncImports() -
                                module.getCodeSection().copiesLength + count, module
                ))));
                count++;
            }
        }
    }
}
