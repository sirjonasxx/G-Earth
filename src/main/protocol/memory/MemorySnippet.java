package main.protocol.memory;

public class MemorySnippet {
    long offset;
    byte[] data;

    public MemorySnippet(long offset, byte[] data) {
        this.offset = offset;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public long getOffset() {
        return offset;
    }
}
