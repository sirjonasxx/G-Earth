package wasm.disassembly.conventions;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;
import wasm.disassembly.modules.Module;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CustomVector<B> extends WASMOpCode {

    private List<B> elements;
    private Assembler<B> assembler;

    public CustomVector(BufferedInputStream in, Creator<B> creator, Assembler<B> assembler, Module module) throws IOException, InvalidOpCodeException {
        long length = WUnsignedInt.read(in, 32);
        elements = new ArrayList<>(1);
        for (int i = 0; i < length; i++) {
            elements.add(creator.create(in, module));
        }
        this.assembler = assembler;
    }

    public CustomVector(List<B> elements, Assembler<B> assembler) {
        this.elements = elements;
        this.assembler = assembler;
    }

    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        WUnsignedInt.write(elements.size(), out, 32);
        for (B b : elements) {
            assembler.assemble(b, out);
        }
    }

    public List<B> getElements() {
        return elements;
    }

    public void setElements(List<B> elements) {
        this.elements = elements;
    }
}
