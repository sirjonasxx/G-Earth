package wasm.disassembly.instructions;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;

public interface InstrSupplier {

    Instr get(BufferedInputStream in, InstrType type, Module module) throws IOException, InvalidOpCodeException;

}
