package wasm.disassembly.modules.sections.data;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.sections.Section;
import wasm.disassembly.modules.sections.element.Elem;
import wasm.disassembly.values.WUnsignedInt;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class DataSection extends Section {

    public static final int DATA_SECTION_ID = 11;
    private final byte[] asBytes;
    private long length;

//    private Vector<Data> dataSegments;

    public DataSection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, DATA_SECTION_ID);
//        dataSegments = new Vector<>(in, Data::new, module);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        length = WUnsignedInt.read(in, 32);
        for (int i = 0; i < length; i++) {
            new Data(in, module).assemble(buffer);
        }
        asBytes = buffer.toByteArray();
    }

//    public DataSection(Module module, List<Data> dataSegments) {
//        super(module, DATA_SECTION_ID);
//        this.dataSegments = new Vector<>(dataSegments);
//    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
//        dataSegments.assemble(out);

        WUnsignedInt.write(length, out, 32);
        out.write(asBytes);
    }

//
//    public List<Data> getDataSegments() {
//        return dataSegments.getElements();
//    }
//
//    public void setDataSegments(List<Data> dataSegments) {
//        this.dataSegments = new Vector<>(dataSegments);
//    }
}
