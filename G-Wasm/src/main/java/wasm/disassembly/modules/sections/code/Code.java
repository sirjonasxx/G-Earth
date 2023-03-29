package wasm.disassembly.modules.sections.code;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Code extends WASMOpCode {

    private Func code;

    public Code(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        long sizeInBytes = WUnsignedInt.read(in, 32);   // don't use
        code = new Func(in, module);
    }

    public Code(Func code) {
        this.code = code;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        ByteArrayOutputStream codeBuffer = new ByteArrayOutputStream();
        code.assemble(codeBuffer);
        byte[] codeInBytes = codeBuffer.toByteArray();
        WUnsignedInt.write(codeInBytes.length, out, 32);
        out.write(codeInBytes);
        codeBuffer.close();
    }

    public Func getCode() {
        return code;
    }

    public void setCode(Func code) {
        this.code = code;
    }
}
