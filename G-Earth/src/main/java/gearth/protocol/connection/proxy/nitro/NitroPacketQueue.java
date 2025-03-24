package gearth.protocol.connection.proxy.nitro;

import gearth.protocol.packethandler.nitro.NitroPacketHandler;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class NitroPacketQueue {

    private final NitroPacketHandler packetHandler;
    private final Queue<byte[]> packets;

    public NitroPacketQueue(NitroPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.packets = new LinkedList<>();
    }

    public void enqueue(byte[] b) {
        this.packets.add(b);
    }

    public synchronized void flushAndAct() throws IOException {
        while (!this.packets.isEmpty()) {
            this.packetHandler.act(this.packets.remove());
        }
    }
}
