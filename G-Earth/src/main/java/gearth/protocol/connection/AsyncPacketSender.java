package gearth.protocol.connection;

import gearth.protocol.HPacket;

import java.util.LinkedList;
import java.util.Queue;

public class AsyncPacketSender {

    private final HProxy proxy;
    private final Queue<HPacket> sendToClientAsyncQueue = new LinkedList<>();
    private final Queue<HPacket> sendToServerAsyncQueue = new LinkedList<>();

    AsyncPacketSender(HProxy proxy) {
        this.proxy = proxy;
        new Thread(() -> {
            while (true) {
                HPacket packet;
                synchronized (sendToClientAsyncQueue) {
                    while ((packet = sendToClientAsyncQueue.poll()) != null) {
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
                synchronized (sendToServerAsyncQueue) {
                    while ((packet = sendToServerAsyncQueue.poll()) != null) {
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

    public void sendToClientAsync(HPacket message) {
        synchronized (sendToClientAsyncQueue) {
            sendToClientAsyncQueue.add(message);
        }

    }
    public void sendToServerAsync(HPacket message) {
        synchronized (sendToServerAsyncQueue) {
            sendToServerAsyncQueue.add(message);
        }
    }

    public void clear() {
        synchronized (sendToClientAsyncQueue) {
            sendToClientAsyncQueue.clear();
        }
        synchronized (sendToServerAsyncQueue) {
            sendToServerAsyncQueue.clear();
        }
    }
}
