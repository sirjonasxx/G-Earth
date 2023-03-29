package wasm.disassembly.modules.sections.element;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.indices.TypeIdx;
import wasm.disassembly.modules.sections.Section;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ElementSection extends Section {

    public static final int ELEMENT_SECTION_ID = 9;

//    private Vector<Elem> elementSegments;

    private final byte[] asBytes;
    private long length;

    public ElementSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, ELEMENT_SECTION_ID);
//        elementSegments = new Vector<>(in, Elem::new, module);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        length = WUnsignedInt.read(in, 32);
        for (int i = 0; i < length; i++) {
            new Elem(in, module).assemble(buffer);
        }

        asBytes = buffer.toByteArray();
    }

//    public ElementSection(Module module, List<Elem> elementSegments) {
//        super(module, ELEMENT_SECTION_ID);
//        this.elementSegments = new Vector<>(elementSegments);
//    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
//        elementSegments.assemble(out);

        WUnsignedInt.write(length, out, 32);
        out.write(asBytes);
    }

//    public List<Elem> getElementSegments() {
//        return elementSegments.getElements();
//    }
//
//    public void setElementSegments(List<Elem> elementSegments) {
//        this.elementSegments = new Vector<>(elementSegments);
//    }
}
