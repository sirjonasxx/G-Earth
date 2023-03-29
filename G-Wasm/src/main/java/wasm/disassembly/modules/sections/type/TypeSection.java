package wasm.disassembly.modules.sections.type;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.indices.TypeIdx;
import wasm.disassembly.modules.sections.Section;
import wasm.disassembly.types.FuncType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class TypeSection extends Section {

    public static final int TYPE_SECTION_ID = 1;

    private Vector<FuncType> functionTypes;


    public TypeSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, TYPE_SECTION_ID);
        functionTypes = new Vector<>(in, FuncType::new, module);
    }

    public TypeSection(Module module, List<FuncType> functionTypes) {
        super(module, TYPE_SECTION_ID);
        this.functionTypes = new Vector<>(functionTypes);
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        functionTypes.assemble(out);
    }

//    public FuncType getByFuncIdx(FuncIdx funcIdx) {
//        return getByTypeIdx(module.getFunctionSection().getByIdx(funcIdx));
//    }

    public List<FuncType> getFunctionTypes() {
        return functionTypes.getElements();
    }

    public FuncType getByTypeIdx(TypeIdx typeIdx) {
        return functionTypes.getElements().get((int)(typeIdx.getX()));
    }

    public void setFunctionTypes(List<FuncType> functionTypes) {
        this.functionTypes = new Vector<>(functionTypes);
    }

    public TypeIdx getTypeIdxForFuncType(FuncType newFuncType) {
        for (int i = 0; i < getFunctionTypes().size(); i++) {
            FuncType funcType = getFunctionTypes().get(i);
            if (funcType.equals(newFuncType)) {
                return new TypeIdx(i);
            }
        }

        getFunctionTypes().add(newFuncType);
        return new TypeIdx(getFunctionTypes().size() - 1);
    }
}
