package wasm.disassembly;

public class InvalidOpCodeException extends Exception {

    public InvalidOpCodeException(String message) {
        super(message);
    }
}
