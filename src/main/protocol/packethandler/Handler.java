package main.protocol.packethandler;

import main.protocol.HMessage;
import main.protocol.TrafficListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class Handler {

    volatile PayloadBuffer payloadBuffer = new PayloadBuffer();
    volatile OutputStream out;
    private volatile Object[] listeners = null; //get notified on packet send
    private volatile boolean isTempBlocked = false;
    volatile boolean isDataStream = false;
    volatile int currentIndex = 0;


    public Handler(OutputStream outputStream) {
        out = outputStream;
    }

    public boolean isDataStream() {return isDataStream;}
    public void setAsDataStream() {
        isDataStream = true;
    }

    public void act(byte[] buffer, Object[] listeners) throws IOException {
        this.listeners = listeners;

        if (isDataStream)	{
            payloadBuffer.push(buffer);
            notifyBufferListeners();

            if (!isTempBlocked) {
                flush();
            }
            else {
                if (this instanceof OutgoingHandler) {
                    System.out.println("blocked outgoing bytes with size: "+ buffer.length);
                }
            }
        }
        else  {
            out.write(buffer);
        }
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



    private List<BufferListener> bufferListeners = new ArrayList<>();
    public void addBufferListener(BufferListener listener) {
        bufferListeners.add(listener);
    }
    public void removeBufferListener(BufferListener listener) {
        bufferListeners.remove(listener);
    }
    private void notifyBufferListeners() {
        for (int i = bufferListeners.size() - 1; i >= 0; i -= 1) {
            bufferListeners.get(i).act();
        }
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
