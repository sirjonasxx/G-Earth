package gearth.protocol.packethandler;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.TrafficListener;
import gearth.protocol.crypto.RC4;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class PacketHandler {

    protected static final boolean DEBUG = false;

    private volatile PayloadBuffer payloadBuffer = new PayloadBuffer();
    private volatile OutputStream out;
    private volatile Object[] trafficObservables; //get notified on packet send
    private volatile boolean isTempBlocked = false;
    volatile boolean isDataStream = false;
    private volatile int currentIndex = 0;

    private final Object manipulationLock = new Object();
    private final Object sendLock = new Object();

    private RC4 decryptcipher = null;
    private RC4 encryptcipher = null;

    private volatile List<Byte> tempEncryptedBuffer = new ArrayList<>();
    volatile boolean isEncryptedStream = false;


    PacketHandler(OutputStream outputStream, Object[] trafficObservables) {
        this.trafficObservables = trafficObservables;
        out = outputStream;
    }

    public boolean isDataStream() {return isDataStream;}
    public void setAsDataStream() {
        isDataStream = true;
    }

    public boolean isEncryptedStream() {
        return isEncryptedStream;
    }

    public void act(byte[] buffer) throws IOException {
        if (!isDataStream) {
            out.write(buffer);
            return;
        }

        bufferChangeObservable.fireEvent();

        if (!isEncryptedStream) {
            payloadBuffer.push(buffer);
        }
        else if (!HConnection.DECRYPTPACKETS) {
            synchronized (sendLock) {
                out.write(buffer);
            }
        }
        else if (decryptcipher == null) {
            for (int i = 0; i < buffer.length; i++) {
                tempEncryptedBuffer.add(buffer[i]);
            }
        }
        else {
            byte[] tm = decryptcipher.rc4(buffer);
            if (DEBUG) {
                printForDebugging(tm);
            }
            payloadBuffer.push(tm);
        }

        if (!isTempBlocked) {
            flush();
        }
    }


    public void setRc4(RC4 rc4) {
        this.decryptcipher = rc4.deepCopy();
        this.encryptcipher = rc4.deepCopy();

        byte[] encrbuffer = new byte[tempEncryptedBuffer.size()];
        for (int i = 0; i < tempEncryptedBuffer.size(); i++) {
            encrbuffer[i] = tempEncryptedBuffer.get(i);
        }

        try {
            act(encrbuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tempEncryptedBuffer = null;
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
        for (int x = 0; x < 3; x++) {
            ((Observable<TrafficListener>) trafficObservables[x]).fireEvent(trafficListener -> {
                message.getPacket().resetReadIndex();
                trafficListener.onCapture(message);
            });
        }
        message.getPacket().resetReadIndex();
    }

    public void sendToStream(byte[] buffer) {
        synchronized (sendLock) {
            try {
                out.write(
                        (!isEncryptedStream)
                                ? buffer
                                : encryptcipher.rc4(buffer)
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void flush() throws IOException {
        synchronized (manipulationLock) {
            HPacket[] hpackets = payloadBuffer.receive();

            for (HPacket hpacket : hpackets){
                HMessage hMessage = new HMessage(hpacket, getMessageSide(), currentIndex);
                boolean isencrypted = isEncryptedStream;
                if (isDataStream) {
                    notifyListeners(hMessage);
                }

                if (!hMessage.isBlocked())	{
                    synchronized (sendLock) {
                        out.write(
                                (!isencrypted)
                                        ? hMessage.getPacket().toBytes()
                                        : encryptcipher.rc4(hMessage.getPacket().toBytes())
                        );
                    }
                }
                currentIndex++;
            }
        }
    }

    public abstract HMessage.Direction getMessageSide();

    public List<Byte> getEncryptedBuffer() {
        return tempEncryptedBuffer;
    }

    protected abstract void printForDebugging(byte[] bytes);

    private Observable<BufferChangeListener> bufferChangeObservable = new Observable<>(BufferChangeListener::act);
    public Observable<BufferChangeListener> getBufferChangeObservable() {
        return bufferChangeObservable;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
