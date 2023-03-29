package wasm.disassembly.modules.sections.function;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.TypeIdx;
import wasm.disassembly.modules.sections.Section;
import wasm.disassembly.values.WUnsignedInt;
import wasm.misc.StreamReplacement;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FunctionSection extends Section {

    public static final int FUNCTION_SECTION_ID = 3;

//    private Vector<TypeIdx> typeIdxVector;
    private final byte[] asBytes;
    private long length;

    public List<Set<Integer>> matchesSearchFunctionsTypes = new ArrayList<>();


    public FunctionSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, FUNCTION_SECTION_ID);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        List<TypeIdx> searchFunctions = new ArrayList<>();
        for (int i = 0; i < module.streamReplacements.size(); i++) {
            StreamReplacement f = module.streamReplacements.get(i);
            searchFunctions.add(module.getTypeSection().getTypeIdxForFuncType(f.getFuncType()));
            matchesSearchFunctionsTypes.add(new HashSet<>());
        }

//        typeIdxVector = new Vector<>(in, TypeIdx::new, module);
        length = WUnsignedInt.read(in, 32);
        for (int i = 0; i < length; i++) {
            TypeIdx typeIdx = new TypeIdx(in, module);
            for (int j = 0; j < searchFunctions.size(); j++) {
                if (typeIdx.equals(searchFunctions.get(j))) {
                    matchesSearchFunctionsTypes.get(j).add(i);
                }
            }
            typeIdx.assemble(buffer);
        }

        for (int i = 0; i < module.streamReplacements.size(); i++) {
            StreamReplacement.ReplacementType actionTaken = module.streamReplacements.get(i).getReplacementType();
            // new function will be created
            if (actionTaken == StreamReplacement.ReplacementType.HOOKCOPYEXPORT) {
                searchFunctions.get(i).assemble(buffer);
                length++;
            }
        }

        asBytes = buffer.toByteArray();
    }

//    public FunctionSection(Module module, List<TypeIdx> typeIdxList) {
//        super(module, FUNCTION_SECTION_ID);
//        this.typeIdxVector = new Vector<>(typeIdxList);
//    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
//        typeIdxVector.assemble(out);
        WUnsignedInt.write(length, out, 32);
        out.write(asBytes);
    }

//    public TypeIdx getByIdx(FuncIdx funcIdx) {
//        return typeIdxVector.getElements().get((int)(funcIdx.getX()) - module.getImportSection().getTotalFuncImports());
//    }
//
//    public List<TypeIdx> getTypeIdxVector() {
//        return typeIdxVector.getElements();
//    }
//
//    public void setTypeIdxVector(List<TypeIdx> typeIdxVector) {
//        this.typeIdxVector = new Vector<>(typeIdxVector);
//    }

}
