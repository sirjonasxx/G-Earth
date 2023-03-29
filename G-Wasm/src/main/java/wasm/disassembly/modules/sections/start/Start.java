package wasm.disassembly.modules.sections.start;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.FuncIdx;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Start extends WASMOpCode {

    private FuncIdx funcIdx;

    public Start(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        funcIdx = new FuncIdx(in, module);
    }

    public Start(FuncIdx funcIdx) {
        this.funcIdx = funcIdx;
    }


    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        funcIdx.assemble(out);
    }

    public FuncIdx getFuncIdx() {
        return funcIdx;
    }

    public void setFuncIdx(FuncIdx funcIdx) {
        this.funcIdx = funcIdx;
    }
}
