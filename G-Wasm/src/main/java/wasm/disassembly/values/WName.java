package wasm.disassembly.values;

import wasm.disassembly.InvalidOpCodeException;
import wasm.disassembly.WASMOpCode;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class WName {

    public static String read(BufferedInputStream in) throws IOException, InvalidOpCodeException {
        long length = WUnsignedInt.read(in, 32);
        byte[] arr = new byte[(int)length];
        in.read(arr);
        return new String(arr, StandardCharsets.UTF_8);
    }

    public static void write(String value, OutputStream out) throws IOException, InvalidOpCodeException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        WUnsignedInt.write(bytes.length, out, 32);
        out.write(bytes);
    }
}
