package wasm.disassembly.modules.sections.memory;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.conventions.Vector;
import wasm.disassembly.modules.Module;
import wasm.disassembly.modules.sections.Section;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class MemorySection extends Section {

    public static final int MEMORY_SECTION_ID = 5;

    private Vector<Mem> memories;


    public MemorySection(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException {
        super(in, module, MEMORY_SECTION_ID);
        memories = new Vector<>(in, Mem::new, module);
    }

    public MemorySection(Module module, List<Mem> memories) {
        super(module, MEMORY_SECTION_ID);
        this.memories = new Vector<>(memories);
    }

    @Override
    protected void assemble2(OutputStream out) throws IOException, InvalidOpCodeException {
        memories.assemble(out);
    }


    public List<Mem> getMemories() {
        return memories.getElements();
    }

    public void setMemories(List<Mem> memories) {
        this.memories = new Vector<>(memories);
    }
}
