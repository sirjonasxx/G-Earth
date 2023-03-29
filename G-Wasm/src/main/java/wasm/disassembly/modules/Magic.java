package wasm.disassembly.modules;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class Magic extends WASMOpCode {

    private static final byte[] EXPECTED_MAGIC = new byte[]{0x00, 0x61, 0x73, 0x6D};


    public Magic(BufferedInputStream in) throws IOException, InvalidOpCodeException {
        byte[] magic = new byte[4];
        in.read(magic);

        if (!Arrays.equals(magic, EXPECTED_MAGIC)) {
            throw new InvalidOpCodeException("Invalid magic");
        }
    }

    public Magic() {}

    @Override
    public void assemble(OutputStream out) throws IOException, InvalidOpCodeException {
        out.write(EXPECTED_MAGIC);
    }
}
