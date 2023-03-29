package wasm.misc;

import wasm.disassembly.instructions.Expression;
import wasm.disassembly.modules.sections.code.Locals;
import wasm.disassembly.types.FuncType;
import wasm.disassembly.types.ValType;

import java.util.ArrayList;
import java.util.List;

public class Function {

    private FuncType funcType = null;
    private List<Locals> locals = null;
    private Expression code = null;

    public Function(FuncType funcType, List<Locals> locals, Expression code) {
        this.funcType = funcType;
        this.locals = locals;
        this.code = code;
    }

//    public Function(Module module, FuncIdx funcIdx) {
//        funcType = module.getTypeSection().getByFuncIdx(funcIdx);
//
//        Func code = module.getCodeSection().getByIdx(funcIdx);
//        this.code = code.getExpression();
//        locals = code.getLocalss();
//    }

//    public FuncIdx addToModule(Module module) {
//        TypeIdx typeIdx = module.getTypeSection().getTypeIdxForFuncType(funcType);
//        Func func = new Func(locals, code);
//
//        module.getFunctionSection().getTypeIdxVector().add(typeIdx);
//        module.getCodeSection().getCodesEntries().add(new Code(func));
//        return new FuncIdx(
//                module.getImportSection().getTotalFuncImports() + module.getCodeSection().getCodesEntries().size() - 1,
//                module
//        );
//    }


    public FuncType getFuncType() {
        return funcType;
    }

    public void setFuncType(FuncType funcType) {
        this.funcType = funcType;
    }

    public List<Locals> getLocals() {
        return locals;
    }

    public List<ValType> getLocalsFloored() {
        List<ValType> result = new ArrayList<>();
        for (Locals loc : locals) {
            for (int i = 0; i < loc.getAmount(); i++) {
                result.add(loc.getValType());
            }
        }

        return result;
    }

    public void setLocals(List<Locals> locals) {
        this.locals = locals;
    }

    public Expression getCode() {
        return code;
    }

    public void setCode(Expression code) {
        this.code = code;
    }
}
