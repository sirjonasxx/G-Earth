package main.ui.extensions;

import javafx.beans.InvalidationListener;
import main.protocol.HMessage;
import main.protocol.HPacket;
import main.protocol.packethandler.PayloadBuffer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
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
                synchronized (connection) {
                    connection.getOutputStream().write((new HPacket(Extensions.OUTGOING_MESSAGES_IDS.INFOREQUEST)).toBytes());
                }

                InputStream inputStream = connection.getInputStream();
                DataInputStream dIn = new DataInputStream(inputStream);

                while (!connection.isClosed()) {

                    int length = dIn.readInt();
                    byte[] headerandbody = new byte[length + 4];

                    int amountRead = 0;
                    while (amountRead < length) {
                        amountRead += dIn.read(headerandbody, 4 + amountRead, Math.min(dIn.available(), length - amountRead));
                    }

                    HPacket packet = new HPacket(headerandbody);
                    packet.fixLength();

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
                        break;
                    }
                }

            } catch (IOException ignored) {}
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
                InputStream inputStream = connection.getInputStream();
                DataInputStream dIn = new DataInputStream(inputStream);

                while (!connection.isClosed()) {
                    int length = dIn.readInt();
                    byte[] headerandbody = new byte[length + 4];

                    int amountRead = 0;
                    while (amountRead < length) {
                        amountRead += dIn.read(headerandbody, 4 + amountRead, Math.min(dIn.available(), length - amountRead));
                    }

                    HPacket packet = new HPacket(headerandbody);
                    packet.fixLength();

                    for (int i = receiveMessageListeners.size() - 1; i >= 0; i--) {
                        receiveMessageListeners.get(i).act(packet);
                        packet.setReadIndex(6);
                    }

                }

            } catch (IOException e) {
                // An extension disconnected, which is OK
            } finally {
                onDisconnectedCallback.act(selff);
                if (!connection.isClosed()) {
                    try {
                        connection.close();
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                }
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
            synchronized (this) {
                connection.getOutputStream().write(message.toBytes());
            }
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


    private List<InvalidationListener> onRemoveClickListener = new ArrayList<>();
    public void onRemoveClick(InvalidationListener listener) {
        onRemoveClickListener.add(listener);
    }
    public void isRemoveClickTrigger() {
        for (int i = onRemoveClickListener.size() - 1; i >= 0; i--) {
            onRemoveClickListener.get(i).invalidated(null);
        }
    }

    private List<InvalidationListener> onClickListener = new ArrayList<>();
    public void onClick(InvalidationListener listener) {
        onClickListener.add(listener);
    }
    public void isClickTrigger() {
        for (int i = onClickListener.size() - 1; i >= 0; i--) {
            onClickListener.get(i).invalidated(null);
        }
    }

    private List<InvalidationListener> onDeleteListeners = new ArrayList<>();
    public void onDelete(InvalidationListener listener) {
        onDeleteListeners.add(listener);
    }
    public void delete() {
        for (int i = onDeleteListeners.size() - 1; i >= 0; i--) {
            onDeleteListeners.get(i).invalidated(null);
        }
    }
}