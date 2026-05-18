package gearth.app.protocol.connection.proxy.nitro;

public class NitroPacketEvent {

    /**
     * The raw packet buffer.
     * This can be a fragment of the full packet.
     */
    public byte[] buffer;

    /**
     * If true, the packet will bypass the G-Earth packet handler and will be sent directly to the client/server.
     * If false, the packet will be processed by the G-Earth packet handler as normal.
     * <p>
     * This only applies to packets coming into G-Earth.
     */
    public boolean bypass;

    /**
     * If true, the packet will be dropped.
     */
    public boolean cancel;

    public NitroPacketEvent(byte[] buffer) {
        this.buffer = buffer;
    }
}
