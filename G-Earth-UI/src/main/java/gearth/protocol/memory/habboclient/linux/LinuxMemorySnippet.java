package gearth.protocol.memory.habboclient.linux;

public class LinuxMemorySnippet {
    long offset;
    byte[] data;

    public LinuxMemorySnippet(long offset, byte[] data) {
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
