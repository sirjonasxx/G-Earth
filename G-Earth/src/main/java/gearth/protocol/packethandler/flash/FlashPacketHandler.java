package gearth.protocol.packethandler.flash;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.TrafficListener;
import gearth.protocol.crypto.RC4;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.PayloadBuffer;
import gearth.services.extensionhandler.ExtensionHandler;
import gearth.services.extensionhandler.OnHMessageHandled;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class FlashPacketHandler extends PacketHandler {

    protected static final boolean DEBUG = false;

    private volatile OutputStream out;
    private volatile boolean isTempBlocked = false;
    volatile boolean isDataStream = false;

    private final Object manipulationLock = new Object();

    private RC4 decryptcipher = null;
    private RC4 encryptcipher = null;

    private volatile List<Byte> tempEncryptedBuffer = new ArrayList<>();
    volatile boolean isEncryptedStream = false;

    private final PayloadBuffer payloadBuffer;


    FlashPacketHandler(OutputStream outputStream, Object[] trafficObservables, ExtensionHandler extensionHandler) {
        super(extensionHandler, trafficObservables);
        out = outputStream;
        this.payloadBuffer = new PayloadBuffer();
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

                OnHMessageHandled afterExtensionIntercept = hMessage1 -> {
                    if (isDataStream) {
                        notifyListeners(2, hMessage1);
                    }

                    if (!hMessage1.isBlocked())	{
                        synchronized (sendLock) {
                            out.write(
                                    (!isencrypted)
                                            ? hMessage1.getPacket().toBytes()
                                            : encryptcipher.rc4(hMessage1.getPacket().toBytes())
                            );
                        }
                    }
                };

                if (isDataStream) {
                    notifyListeners(0, hMessage);
                    notifyListeners(1, hMessage);
                    extensionHandler.handle(hMessage, afterExtensionIntercept);
                }
                else {
                    afterExtensionIntercept.finished(hMessage);
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
