package gearth.app.protocol.connection.proxy.nitro;

import gearth.app.protocol.packethandler.nitro.NitroPacketHandler;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class NitroPacketQueue {

    private final NitroPacketHandler packetHandler;
    private final Queue<Entry> packets;

    public NitroPacketQueue(NitroPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.packets = new LinkedList<>();
    }

    public void enqueue(byte[] b, boolean bypass) {
        this.packets.add(new Entry(b, bypass));
    }

    public synchronized void flushAndAct() throws IOException {
        while (!this.packets.isEmpty()) {
            final Entry entry = this.packets.remove();

            if (entry.bypass) {
                this.packetHandler.sendToStream(entry.packet);
            } else {
                this.packetHandler.act(entry.packet);
            }
        }
    }

    private record Entry(byte[] packet, boolean bypass) {
    }
}
