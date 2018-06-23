package main.ui.extensions;

import main.protocol.HMessage;
import main.protocol.HPacket;
import main.protocol.packethandler.PayloadBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonas on 21/06/18.
 */
public class GEarthExtension {

    private String title;
    private String author;
    private String version;
    private String description;

    private Socket connection;

    //calls callback when the extension is creatd
    static void create(Socket connection, OnCreatedCallback callback, OnDisconnectedCallback onDisconnectedCallback) {

        new Thread(() -> {
            try {
                connection.getOutputStream().write((new HPacket(Extensions.OUTGOING_MESSAGES_IDS.INFOREQUEST)).toBytes());
                connection.getOutputStream().flush();

                PayloadBuffer payloadBuffer = new PayloadBuffer();
                InputStream inputStream = connection.getInputStream();

                outerloop:
                while (!connection.isClosed()) {
                    if (inputStream.available() > 0) {
                        byte[] incoming = new byte[inputStream.available()];
                        inputStream.read(incoming);
                        payloadBuffer.push(incoming);
                    }

                    HPacket[] hPackets = payloadBuffer.receive();
                    for (HPacket packet : hPackets) { // it should be only one packet
                        if (packet.headerId() == Extensions.INCOMING_MESSAGES_IDS.EXTENSIONINFO) {

                            GEarthExtension gEarthExtension = new GEarthExtension(
                                    packet.readString(),
                                    packet.readString(),
                                    packet.readString(),
                                    packet.readString(),
                                    connection,
                                    onDisconnectedCallback
                            );
                            callback.act(gEarthExtension);

                            break outerloop;
                        }
                    }

                    Thread.sleep(1);
                }

            } catch (IOException | InterruptedException ignored) {}
        }).start();

    }

    private GEarthExtension(String title, String author, String version, String description, Socket connection, OnDisconnectedCallback onDisconnectedCallback) {
        this.title = title;
        this.author = author;
        this.version = version;
        this.description = description;
        this.connection = connection;

        GEarthExtension selff = this;
        new Thread(() -> {
            try {
                PayloadBuffer payloadBuffer = new PayloadBuffer();
                InputStream inputStream = connection.getInputStream();

                while (!connection.isClosed()) {
                    if (inputStream.available() > 0) {
                        byte[] incoming = new byte[inputStream.available()];
                        inputStream.read(incoming);
                        payloadBuffer.push(incoming);
                    }

                    HPacket[] hPackets = payloadBuffer.receive();
                    for (HPacket packet : hPackets) {
                        for (int i = receiveMessageListeners.size() - 1; i >= 0; i--) {
                            receiveMessageListeners.get(i).act(packet);
                        }
                    }

                    Thread.sleep(1);
                }
                onDisconnectedCallback.act(selff);

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();


    }

    public Socket getConnection() {
        return connection;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }



    public boolean closeConnection() {
        try {
            connection.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean sendMessage(HPacket message) {
        try {
            connection.getOutputStream().write(message.toBytes());
            connection.getOutputStream().flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    private List<ReceiveMessageListener> receiveMessageListeners = new ArrayList<>();
    public void addOnReceiveMessageListener(ReceiveMessageListener receiveMessageListener) {
        receiveMessageListeners.add(receiveMessageListener);
    }
    public void removeOnReceiveMessageListener(ReceiveMessageListener receiveMessageListener) {
        receiveMessageListeners.remove(receiveMessageListener);
    }

    public interface ReceiveMessageListener {
        void act(HPacket message);
    }
    public interface OnCreatedCallback {
        void act(GEarthExtension extension); // returns itself
    }
    public interface OnDisconnectedCallback {
        void act(GEarthExtension extension); // returns itself
    }

}