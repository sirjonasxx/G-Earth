package wasm.disassembly.values;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class WFloat {

    public static float read(BufferedInputStream in) throws IOException {
        byte[] bytes = new byte[4];
        in.read(bytes);
        return ByteBuffer.wrap(bytes).getFloat();
    }

    public static void write(float value, OutputStream out) throws IOException {
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).putFloat(value);
        out.write(bytes);
    }

}
