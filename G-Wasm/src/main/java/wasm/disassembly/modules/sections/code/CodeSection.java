package wasm.disassembly.modules.sections.code;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.instructions.Expression;
import wasm.disassembly.instructions.Instr;
import wasm.disassembly.instructions.InstrType;
import wasm.disassembly.instructions.control.CallInstr;
import wasm.disassembly.instructions.variable.LocalVariableInstr;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;
import wasm.disassembly.modules.indices.LocalIdx;
import wasm.disassembly.modules.sections.Section;
import wasm.disassembly.values.WUnsignedInt;
import wasm.misc.StreamReplacement;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CodeSection extends Section {

    public static final int CODE_SECTION_ID = 10;

//    public static int currentI = 0;

    public final byte[] asBytes;
    public long length;
    public int copiesLength;
//    private Vector<Code> codesEntries;

    public CodeSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, CODE_SECTION_ID);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Code[] copies = new Code[module.streamReplacements.size()];

//        codesEntries = new Vector<>(in, Code::new, module);
        length = WUnsignedInt.read(in, 32);
        for (int i = 0; i < length; i++) {
            Code code = new Code(in, module);


            Func func = code.getCode();
            for (int j = 0; j < module.streamReplacements.size(); j++) {

                if (module.getFunctionSection().matchesSearchFunctionsTypes.get(j).contains(i)) {

                    if (module.streamReplacements.get(j).codeMatches(func)) {
                        StreamReplacement.ReplacementType actionTaken = module.streamReplacements.get(j).getReplacementType();
                        if (actionTaken == StreamReplacement.ReplacementType.HOOK) {
                            CallInstr call = new CallInstr(new FuncIdx(j, module));

                            List<Instr> newInstrs = new ArrayList<>();
                            for (int k = 0; k < module.streamReplacements.get(j).getFuncType().getParameterType().typeList().size(); k++) {
                                newInstrs.add(new LocalVariableInstr(InstrType.LOCAL_GET, new LocalIdx(k)));
                            }
                            newInstrs.add(call);
                            newInstrs.addAll(func.getExpression().getInstructions());
                            func.getExpression().setInstructions(newInstrs);
                        }
                        else if (actionTaken == StreamReplacement.ReplacementType.HOOKCOPYEXPORT) {
                            copies[j] = new Code(new Func(func.getLocalss(), new Expression(func.getExpression().getInstructions())));

                            CallInstr call = new CallInstr(new FuncIdx(j, module));
                            List<Instr> newInstrs = new ArrayList<>();
                            for (int k = 0; k < module.streamReplacements.get(j).getFuncType().getParameterType().typeList().size(); k++) {
                                newInstrs.add(new LocalVariableInstr(InstrType.LOCAL_GET, new LocalIdx(k)));
                            }
                            newInstrs.add(call);
                            func.getExpression().setInstructions(newInstrs);
                        }
                    }

                }
            }


            code.assemble(buffer);
        }

        for (Code code : copies) {
            if (code != null) {
                code.assemble(buffer);
                length++;
                copiesLength++;
            }
        }

        asBytes = buffer.toByteArray();
    }

//    public CodeSection(Module module, List<Code> codesEntries) {
//        super(module, CODE_SECTION_ID);
//        this.codesEntries = new Vector<>(codesEntries);
//    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
//        codesEntries.assemble(out);

        WUnsignedInt.write(length, out, 32);
        out.write(asBytes);
    }

//    public Code getCodeByIdx(FuncIdx funcIdx) {
//        return codesEntries.getElements().get((int)(funcIdx.getX()) - module.getImportSection().getTotalFuncImports());
//    }
//
//    public Func getByIdx(FuncIdx funcIdx) {
//        return getCodeByIdx(funcIdx).getCode();
//    }
//
//    public List<Code> getCodesEntries() {
//        return codesEntries.getElements();
//    }
//
//    public void setCodesEntries(List<Code> codesEntries) {
//        this.codesEntries = new Vector<>(codesEntries);
//    }
}
