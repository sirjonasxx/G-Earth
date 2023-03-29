package wasm.disassembly.modules.sections.custom.namesection.subsections;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.values.WName;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class SubSection extends WASMOpCode {

    private long size;
    private int subSectionId;

    public SubSection(BufferedInputStream in, int subSectionId) throws IOException, InvalidOpCodeException {
        this.subSectionId = subSectionId;
        size = WUnsignedInt.read(in, 32);
    }

    public SubSection(int subSectionId) {
        this.subSectionId = subSectionId;
        size = -1;
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(subSectionId);

        ByteArrayOutputStream fakeOutputStream = new ByteArrayOutputStream();
        assemble2(fakeOutputStream);
        byte[] asbytes = fakeOutputStream.toByteArray();
        WUnsignedInt.write(asbytes.length, out, 32);
        out.write(asbytes);
    }

    protected abstract void assemble2(OutputStream out) throws IOException, InvalidOpCodeException;

    public int getSubSectionId() {
        return subSectionId;
    }
}
