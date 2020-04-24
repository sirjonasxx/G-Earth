package gearth.ui.extensions;

import javafx.beans.InvalidationListener;
import gearth.protocol.HPacket;

import java.io.*;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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

    private boolean fireEventButtonVisible;
    private boolean leaveButtonVisible;
    private boolean deleteButtonVisible;

    private boolean isInstalledExtension; // <- extension is in the extensions directory
    private String fileName;
    private String cookie;

    private OnDisconnectedCallback disconnectedCallback;

    private SocketChannel connection;

    //calls callback when the extension is created
    GEarthExtension(HPacket extensionInfo, SocketChannel connection, OnDisconnectedCallback onDisconnectedCallback) {
        this.title = extensionInfo.readString();
        this.author = extensionInfo.readString();
        this.version = extensionInfo.readString();
        this.description = extensionInfo.readString();
        this.fireEventButtonVisible = extensionInfo.readBoolean();

        this.isInstalledExtension = extensionInfo.readBoolean();
        this.fileName = extensionInfo.readString();
        this.cookie = extensionInfo.readString();

        this.leaveButtonVisible = extensionInfo.readBoolean();
        this.deleteButtonVisible = extensionInfo.readBoolean();

        this.connection = connection;
        this.disconnectedCallback = onDisconnectedCallback;
    }

    public void act(HPacket packet) {
        synchronized (receiveMessageListeners) {
            for (int i = receiveMessageListeners.size() - 1; i >= 0; i--) {
                receiveMessageListeners.get(i).act(packet);
                packet.setReadIndex(6);
            }
        }
    }

    void disconnect() {
        disconnectedCallback.act(this);
    }

    public SocketChannel getConnection() {
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
    public boolean isFireButtonUsed() {
        return fireEventButtonVisible;
    }
    public String getFileName() {
        return fileName;
    }
    public String getCookie() {
        return cookie;
    }
    public boolean isDeleteButtonVisible() {
        return deleteButtonVisible;
    }
    public boolean isLeaveButtonVisible() {
        return leaveButtonVisible;
    }

    public boolean isInstalledExtension() {
        return isInstalledExtension;
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
                connection.write(ByteBuffer.wrap(message.toBytes()));
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    private final List<ReceiveMessageListener> receiveMessageListeners = new ArrayList<>();
    public void addOnReceiveMessageListener(ReceiveMessageListener receiveMessageListener) {
        synchronized (receiveMessageListeners) {
            receiveMessageListeners.add(receiveMessageListener);
        }
    }
    public void removeOnReceiveMessageListener(ReceiveMessageListener receiveMessageListener) {
        synchronized (receiveMessageListeners) {
            receiveMessageListeners.remove(receiveMessageListener);
        }
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


    private final List<InvalidationListener> onRemoveClickListener = new ArrayList<>();
    public void onRemoveClick(InvalidationListener listener) {
        synchronized (onRemoveClickListener) {
            onRemoveClickListener.add(listener);
        }
    }
    public void isRemoveClickTrigger() {
        synchronized (onRemoveClickListener) {
            for (int i = onRemoveClickListener.size() - 1; i >= 0; i--) {
                onRemoveClickListener.get(i).invalidated(null);
            }
        }
    }

    private final List<InvalidationListener> onClickListener = new ArrayList<>();
    public void onClick(InvalidationListener listener) {
        synchronized (onClickListener) {
            onClickListener.add(listener);
        }
    }
    public void isClickTrigger() {
        synchronized (onClickListener) {
            for (int i = onClickListener.size() - 1; i >= 0; i--) {
                onClickListener.get(i).invalidated(null);
            }
        }
    }

    private final List<InvalidationListener> onDeleteListeners = new ArrayList<>();
    public void onDelete(InvalidationListener listener) {
        synchronized (onDeleteListeners) {
            onDeleteListeners.add(listener);
        }
    }
    public void delete() {
        synchronized (onDeleteListeners) {
            for (int i = onDeleteListeners.size() - 1; i >= 0; i--) {
                onDeleteListeners.get(i).invalidated(null);
            }
        }
    }
}