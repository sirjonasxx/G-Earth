package wasm.disassembly.modules.sections;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class Section extends WASMOpCode {

    private int sectionId;
    private long size;
    protected Module module;

    public Section(BufferedInputStream in, Module module, int sectionId) throws IOException, InvalidOpCodeException {
        this.module = module;
        this.sectionId = sectionId;
        size = WUnsignedInt.read(in, 32);
    }

    public Section(Module module, int sectionId, long size) {
        this.module = module;
        this.sectionId = sectionId;
        this.size = size;
    }

    public Section(Module module, int sectionId) {
        this(module, sectionId, -1);
    }

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(sectionId);

        ByteArrayOutputStream fakeOutputStream = new ByteArrayOutputStream();
        assemble2(fakeOutputStream);
        byte[] asbytes = fakeOutputStream.toByteArray();
        WUnsignedInt.write(asbytes.length, out, 32);
        fakeOutputStream.close();
        out.write(asbytes);
    }

    protected abstract void assemble2(OutputStream out) throws IOException, InvalidOpCodeException;


    public int getSectionId() {
        return sectionId;
    }

    public long getSize() {
        return size;
    }
}
