package wasm.disassembly.values;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class WDouble {

    public static double read(BufferedInputStream in) throws IOException {
        byte[] bytes = new byte[8];
        in.read(bytes);
        return ByteBuffer.wrap(bytes).getDouble();
    }

    public static void write(double value, OutputStream out) throws IOException {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        out.write(bytes);
    }

}
