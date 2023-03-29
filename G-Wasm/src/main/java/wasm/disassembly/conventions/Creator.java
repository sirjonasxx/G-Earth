package wasm.disassembly.conventions;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.modules.Module;

import java.io.BufferedInputStream;
import java.io.IOException;

public interface Creator<B> {

    B create(BufferedInputStream in, Module module) throws IOException, InvalidOpCodeException;

}
