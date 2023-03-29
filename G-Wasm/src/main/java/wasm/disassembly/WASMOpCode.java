package wasm.disassembly;

import java.io.*;

public abstract class WASMOpCode {

    public abstract void assemble(OutputStream out) throws IOException, InvalidOpCodeException;

}
