package main.protocol.packethandler;

import main.protocol.HMessage;
import main.protocol.TrafficListener;
import main.protocol.crypto.RC4;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class Handler {

    protected static final boolean DEBUG = false;

    volatile PayloadBuffer payloadBuffer = new PayloadBuffer();
    volatile OutputStream out;
    volatile Object[] listeners = null; //get notified on packet send
    volatile boolean isTempBlocked = false;
    volatile boolean isDataStream = false;
    volatile int currentIndex = 0;

    protected RC4 clientcipher = null;
    protected RC4 servercipher = null;


    public Handler(OutputStream outputStream, Object[] listeners) {
        this.listeners = listeners;
        out = outputStream;
    }

    public boolean isDataStream() {return isDataStream;}
    public void setAsDataStream() {
        isDataStream = true;
    }

    public abstract void act(byte[] buffer) throws IOException;

    public void setRc4(RC4 rc4) {
        this.clientcipher = rc4.deepCopy();
        this.servercipher = rc4.deepCopy();
    }

    public void block() {
        isTempBlocked = true;
    }
    public void unblock() {
        try {
            flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isTempBlocked = false;
    }

    /**
     * LISTENERS CAN EDIT THE MESSAGE BEFORE BEING SENT
     * @param message
     */
    void notifyListeners(HMessage message) {
        for (TrafficListener listener : (List<TrafficListener>)listeners[0]) {
            listener.onCapture(message);
        }
        for (TrafficListener listener : (List<TrafficListener>)listeners[1]) {
            listener.onCapture(message);
        }
        for (TrafficListener listener : (List<TrafficListener>)listeners[2]) {
            listener.onCapture(message);
        }
    }
    public abstract void sendToStream(byte[] buffer);

    public abstract void flush() throws IOException;

    protected abstract void printForDebugging(byte[] bytes);

    private List<BufferListener> bufferListeners = new ArrayList<>();
    public void addBufferListener(BufferListener listener) {
        bufferListeners.add(listener);
    }
    public void removeBufferListener(BufferListener listener) {
        bufferListeners.remove(listener);
    }
    void notifyBufferListeners(int addedbytes) {
        for (int i = bufferListeners.size() - 1; i >= 0; i -= 1) {
            bufferListeners.get(i).act(addedbytes);
        }
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
