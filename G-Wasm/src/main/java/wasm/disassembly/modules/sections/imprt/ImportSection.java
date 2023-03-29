package wasm.disassembly.modules.sections.imprt;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.sections.Section;
import wasm.disassembly.types.FuncType;
import wasm.disassembly.types.ResultType;
import wasm.misc.StreamReplacement;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportSection extends Section {

    public static final int IMPORT_SECTION_ID = 2;
    private Module module;

    private int totalFuncImports;

    private Vector<Import> imports;


    public ImportSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, IMPORT_SECTION_ID);
        this.module = module;
        imports = new Vector<>(in, Import::new, module);

        totalFuncImports = 0;

        int first = -1;
        for (int i = 0; i < imports.getElements().size(); i++) {
            Import imprt = imports.getElements().get(i);

            if (imprt.getImportDescription().getImportType() == 0) {
                if (totalFuncImports == 0) {
                    first = i;
                }
                totalFuncImports++;
            }
        }

        List<Import> newImports = new ArrayList<>();



        for (StreamReplacement streamReplacement : module.streamReplacements) {
            newImports.add(new Import("env", streamReplacement.getImportName(), new ImportDesc(
                    module.getTypeSection().getTypeIdxForFuncType(new FuncType(
                            streamReplacement.getFuncType().getParameterType(),
                            streamReplacement.getReplacementType() == StreamReplacement.ReplacementType.HOOK ? new ResultType(Collections.emptyList()) :
                                    streamReplacement.getFuncType().getResultType()
                    ))
            )));
        }

        List<Import> f = new ArrayList<>(imports.getElements().subList(0, first));
        f.addAll(newImports);
        f.addAll(imports.getElements().subList(first, imports.getElements().size()));
        imports.setElements(f);
        totalFuncImports += newImports.size();
    }

    public ImportSection(Module module, List<Import> imports) {
        super(module, IMPORT_SECTION_ID);
        this.imports = new Vector<>(imports);
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        imports.assemble(out);
    }

    public List<Import> getImports() {
        return imports.getElements();
    }

    public void setImports(List<Import> imports) {
        this.imports = new Vector<>(imports);
    }

//    public List<FuncIdx> importFunctions(List<Import> functions) throws InvalidOpCodeException {
//        for (Import imprt : functions) {
//            if (imprt.getImportDescription().getImportType() != 0) {
//                throw new InvalidOpCodeException("Tried to import non-function as function");
//            }
//        }
//
//        for (FuncIdx funcIdx : module.getAllFuncIdxs()) {
//            if (funcIdx.getX() >= totalFuncImports) {
//                funcIdx.setX(funcIdx.getX() + functions.size());
//            }
//        }
//        List<FuncIdx> newFuncIdxs = new ArrayList<>();
//        for (long i = totalFuncImports; i < totalFuncImports + functions.size(); i++) {
//            newFuncIdxs.add(new FuncIdx(i, module));
//        }
//        imports.getElements().addAll(functions);
//        totalFuncImports += functions.size();
//        return newFuncIdxs;
//    }

//    public FuncIdx importFunction(Import function) throws InvalidOpCodeException {
//        return importFunctions(Collections.singletonList(function)).get(0);
//    }

    public Import getByIdx(FuncIdx funcIdx) {
        return imports.getElements().get((int)funcIdx.getX());
    }

    public int getTotalFuncImports() {
        return totalFuncImports;
    }
}
