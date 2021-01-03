//package gearth.services.unity_tools;
//
//import wasm.disassembly.InvalidOpCodeException;
//import wasm.disassembly.instructions.Expression;
//import wasm.disassembly.instructions.Instr;
//import wasm.disassembly.instructions.InstrType;
//import wasm.disassembly.instructions.control.CallInstr;
//import wasm.disassembly.instructions.variable.LocalVariableInstr;
//import wasm.disassembly.modules.Module;
//import wasm.disassembly.modules.indices.FuncIdx;
//import wasm.disassembly.modules.indices.LocalIdx;
//import wasm.disassembly.modules.indices.TypeIdx;
//import wasm.disassembly.modules.sections.code.Func;
//import wasm.disassembly.modules.sections.export.Export;
//import wasm.disassembly.modules.sections.export.ExportDesc;
//import wasm.disassembly.modules.sections.imprt.Import;
//import wasm.disassembly.modules.sections.imprt.ImportDesc;
//import wasm.disassembly.types.FuncType;
//import wasm.disassembly.types.ResultType;
//import wasm.disassembly.types.ValType;
//import wasm.misc.Function;
//
//import java.io.*;
//import java.util.*;
//
//public class WasmCodePatcherOld {
//
//    private String file;
//
//    public WasmCodePatcherOld(String file) {
//        this.file = file;
//    }
//
//    public void patch() throws IOException, InvalidOpCodeException {
//        Module module = new Module(file);
//
//        FuncIdx returnByteId = findReturnByteFunc(module);
//        FuncIdx setkey = findSetKeyFunc(module);
//        FuncIdx outgoingIdx = findOutFunc(module);
//        FuncIdx incomingIdx = findInFunc(module);
//
//        hook(module, setkey, "g_chacha_setkey");
//        copyEmptyHook(module, returnByteId, "_gearth_returnbyte_copy", "g_chacha_returnbyte");
//        copyEmptyHook(module, outgoingIdx, "_gearth_outgoing_copy", "g_outgoing_packet");
//        copyEmptyHook(module, incomingIdx, "_gearth_incoming_copy", "g_incoming_packet");
//
//        module.assembleToFile(file);
//    }
//
//
//    private FuncIdx findOutFunc(Module module) {
//        TypeIdx expectedTypeIdx = module.getTypeSection().getTypeIdxForFuncType(new FuncType(
//                new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32)),
//                new ResultType(Collections.emptyList())
//        ));
//
//        outerloop:
//        for (int i = 0; i < module.getCodeSection().getCodesEntries().size(); i++) {
//            FuncIdx currentIdx = new FuncIdx(i + module.getImportSection().getTotalFuncImports(), module);
//
//            Func func = module.getCodeSection().getByIdx(currentIdx);
//            if (func.getLocalss().size() != 0) continue;
//            if (!module.getFunctionSection().getByIdx(currentIdx).equals(expectedTypeIdx)) continue;
//
//            List<Instr> expression = func.getExpression().getInstructions();
//
//            if (expression.size() != 6) continue;
//
//            if (expression.get(0).getInstrType() != InstrType.LOCAL_GET) continue;
//            if (expression.get(1).getInstrType() != InstrType.LOCAL_GET) continue;
//            if (expression.get(2).getInstrType() != InstrType.LOCAL_GET) continue;
//            if (expression.get(3).getInstrType() != InstrType.I32_LOAD) continue;
//            if (expression.get(4).getInstrType() != InstrType.I32_CONST) continue;
//            if (expression.get(5).getInstrType() != InstrType.CALL) continue;
//
//            return currentIdx;
//        }
//
//        return null;
//    }
//    private FuncIdx findSetKeyFunc(Module module) {
//        FuncType expectedType = new FuncType(
//                new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32, ValType.I32)),
//                new ResultType(Collections.emptyList())
//        );
//
//        List<InstrType> expectedExpr = Arrays.asList(InstrType.I32_CONST, InstrType.I32_LOAD8_S,
//                InstrType.I32_EQZ, InstrType.IF, InstrType.BLOCK, InstrType.LOCAL_GET, InstrType.I32_CONST,
//                InstrType.LOCAL_GET, InstrType.I32_LOAD, InstrType.I32_CONST, InstrType.I32_CONST, InstrType.I32_CONST,
//                InstrType.CALL);
//
//        outerloop:
//        for (int i = 0; i < module.getCodeSection().getCodesEntries().size(); i++) {
//            FuncIdx funcIdx = new FuncIdx(i + module.getImportSection().getTotalFuncImports(), module);
//
//            Function function = new Function(module, funcIdx);
//            if (!function.getFuncType().equals(expectedType)) continue;
//            if (!(function.getLocalsFloored().size() == 1 && function.getLocalsFloored().get(0) == ValType.I32)) continue;
//            if (function.getCode().getInstructions().size() != expectedExpr.size()) continue;
//
//            for (int j = 0; j < function.getCode().getInstructions().size(); j++) {
//                Instr instr = function.getCode().getInstructions().get(j);
//                if (instr.getInstrType() != expectedExpr.get(j)) continue outerloop;
//            }
//
//            return funcIdx;
//        }
//
//        return null;
//    }
//    private FuncIdx findReturnByteFunc(Module module) {
//        FuncType expectedType = new FuncType(
//                new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32)),
//                new ResultType(Collections.singletonList(ValType.I32))
//        );
//
//        outerloop:
//        for (int i = 0; i < module.getCodeSection().getCodesEntries().size(); i++) {
//            FuncIdx funcIdx = new FuncIdx(i + module.getImportSection().getTotalFuncImports(), module);
//
//            Function function = new Function(module, funcIdx);
//            if (!function.getFuncType().equals(expectedType)) continue;
//            if (function.getLocalsFloored().size() != 0) continue;
//            if (function.getCode().getInstructions().size() != 30) continue;
//
//            List<Instr> expr = function.getCode().getInstructions();
//            if (expr.get(expr.size() - 1).getInstrType() != InstrType.I32_XOR) continue;
//
//            return funcIdx;
//        }
//
//        return null;
//    }
//    private FuncIdx findInFunc(Module module) {
//        FuncType expectedType = new FuncType(
//                new ResultType(Arrays.asList(ValType.I32, ValType.I32, ValType.I32, ValType.I32, ValType.I32)),
//                new ResultType(Collections.emptyList())
//        );
//
//        List<InstrType> expectedExpr = Arrays.asList(InstrType.I32_CONST, InstrType.I32_LOAD8_S,
//                InstrType.I32_EQZ, InstrType.IF, InstrType.LOCAL_GET, InstrType.I32_LOAD, InstrType.LOCAL_TEE,
//                InstrType.IF);
//
//        outerloop:
//        for (int i = 0; i < module.getCodeSection().getCodesEntries().size(); i++) {
//            FuncIdx funcIdx = new FuncIdx(i + module.getImportSection().getTotalFuncImports(), module);
//
//            Function function = new Function(module, funcIdx);
//            if (!function.getFuncType().equals(expectedType)) continue;
//            if (!(function.getLocalsFloored().size() == 1 && function.getLocalsFloored().get(0) == ValType.I32)) continue;
//            if (function.getCode().getInstructions().size() != expectedExpr.size()) continue;
//
//            for (int j = 0; j < function.getCode().getInstructions().size(); j++) {
//                Instr instr = function.getCode().getInstructions().get(j);
//                if (instr.getInstrType() != expectedExpr.get(j)) continue outerloop;
//            }
//
//            return funcIdx;
//        }
//
//        return null;
//    }
//
//    private void copyEmptyHook(Module module, FuncIdx orgFuncIdx, String exportName, String hookname) throws InvalidOpCodeException, IOException {
//        // copies the method, empties the first one
//        // export the copy
//        // hooks to the emptied one
//
//        Func func = module.getCodeSection().getByIdx(orgFuncIdx);
//        FuncType funcType = module.getTypeSection().getByFuncIdx(orgFuncIdx);
//
//        // copy the function
//        Function copy = new Function(funcType, func.getLocalss(), func.getExpression());
//        FuncIdx copyIdx = copy.addToModule(module);
//
//        module.getExportSection().getExports().add(new Export(exportName, new ExportDesc(copyIdx)));
//
//
//        // clear & hook original function, let it return whatever JS returns
//        Import imp = new Import(
//                "env",
//                hookname,
//                new ImportDesc(module.getTypeSection().getTypeIdxForFuncType(new FuncType(
//                        funcType.getParameterType(),
//                        funcType.getResultType()
//                )))
//        );
//        FuncIdx hookIdx = module.getImportSection().importFunction(imp);
//
//        CallInstr call = new CallInstr(hookIdx);
//        List<Instr> newInstrs = new ArrayList<>();
//        for (int i = 0; i < funcType.getParameterType().typeList().size(); i++) {
//            newInstrs.add(new LocalVariableInstr(InstrType.LOCAL_GET, new LocalIdx(i)));
//        }
//        newInstrs.add(call);
//        func.setExpression(new Expression(newInstrs));
//
//    }
//
//    private void hook(Module module, FuncIdx funcIdx, String jsFunctionName) throws InvalidOpCodeException, IOException {
//        FuncType funcType = module.getTypeSection().getByFuncIdx(funcIdx);
//
//        Import imp = new Import(
//                "env",
//                jsFunctionName,
//                new ImportDesc(module.getTypeSection().getTypeIdxForFuncType(new FuncType(
//                        funcType.getParameterType(),
//                        new ResultType(Collections.emptyList())
//                )))
//        );
//        FuncIdx hookIdx = module.getImportSection().importFunction(imp);
//
//        CallInstr call = new CallInstr(hookIdx);
//
//        Func root = module.getCodeSection().getByIdx(funcIdx);
//        List<Instr> newInstrs = new ArrayList<>();
//        for (int i = 0; i < funcType.getParameterType().typeList().size(); i++) {
//            newInstrs.add(new LocalVariableInstr(InstrType.LOCAL_GET, new LocalIdx(i)));
//        }
//        newInstrs.add(call);
//        newInstrs.addAll(root.getExpression().getInstructions());
//        root.getExpression().setInstructions(newInstrs);
//    }
//
//}
