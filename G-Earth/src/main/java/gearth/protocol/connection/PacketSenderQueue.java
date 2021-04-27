package gearth.protocol.connection;

import gearth.protocol.HPacket;

import java.util.LinkedList;
import java.util.Queue;

public class PacketSenderQueue {

    private final HProxy proxy;
    private final Queue<HPacket> sendToClientQueue = new LinkedList<>();
    private final Queue<HPacket> sendToServerQueue = new LinkedList<>();

    PacketSenderQueue(HProxy proxy) {
        this.proxy = proxy;
        new Thread(() -> {
            while (true) {
                HPacket packet;
                synchronized (sendToClientQueue) {
                    while ((packet = sendToClientQueue.poll()) != null) {
                        sendToClient(packet);
                    }
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                HPacket packet;
                synchronized (sendToServerQueue) {
                    while ((packet = sendToServerQueue.poll()) != null) {
                        sendToServer(packet);
                    }
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void sendToClient(HPacket message) {
        proxy.getInHandler().sendToStream(message.toBytes());
    }
    private void sendToServer(HPacket message) {
        proxy.getOutHandler().sendToStream(message.toBytes());
    }

    public void queueToClient(HPacket message) {
        synchronized (sendToClientQueue) {
            sendToClientQueue.add(message);
        }

    }
    public void queueToServer(HPacket message) {
        synchronized (sendToServerQueue) {
            sendToServerQueue.add(message);
        }
    }

    public void clear() {
        synchronized (sendToClientQueue) {
            sendToClientQueue.clear();
        }
        synchronized (sendToServerQueue) {
            sendToServerQueue.clear();
        }
    }
}
