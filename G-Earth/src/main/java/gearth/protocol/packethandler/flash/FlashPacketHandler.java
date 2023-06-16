package gearth.protocol.packethandler.flash;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.crypto.RC4;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.PayloadBuffer;
import gearth.services.extension_handler.ExtensionHandler;
import gearth.ui.GEarthProperties;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class FlashPacketHandler extends PacketHandler {

    protected static final boolean DEBUG = false;

    private final OutputStream out;
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
        payloadBuffer = new PayloadBuffer();
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
            synchronized (sendLock) {
                out.write(buffer);
            }
            return;
        }

        incomingBufferProperty.set(buffer);

        if (!isEncryptedStream) {
            payloadBuffer.push(buffer);
        }
        else if (GEarthProperties.isPacketDecryptionDisabled()) {
            synchronized (sendLock) {
                out.write(buffer);
            }
        }
        else if (decryptcipher == null) {
            for (byte b : buffer)
                tempEncryptedBuffer.add(b);
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

    public boolean sendToStream(byte[] buffer) {
        return sendToStream(buffer, isEncryptedStream);
    }

    private boolean sendToStream(byte[] buffer, boolean isEncrypted) {
        synchronized (sendLock) {
            try {
                out.write(
                        (!isEncrypted)
                                ? buffer
                                : encryptcipher.rc4(buffer)
                );
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
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
                    awaitListeners(hMessage, hMessage1 -> sendToStream(hMessage1.getPacket().toBytes(), isencrypted));
                }
                else {
                    sendToStream(hMessage.getPacket().toBytes(), isencrypted);
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

    private final ObjectProperty<byte[]> incomingBufferProperty = new SimpleObjectProperty<>();

    public ObjectProperty<byte[]> incomingBufferProperty() {
        return incomingBufferProperty;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
