package wasm.disassembly.conventions;

import java.io.IOException;
import java.io.OutputStream;

public interface Assembler<B> {

    void assemble(B b, OutputStream out) throws IOException;

}
