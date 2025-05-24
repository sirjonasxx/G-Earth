package gearth.app.protocol.packethandler;

import gearth.misc.listenerpattern.Observable;
import gearth.app.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.app.protocol.TrafficListener;
import gearth.app.protocol.crypto.RC4Cipher;
import gearth.app.protocol.packethandler.flash.BufferChangeListener;
import gearth.app.services.extension_handler.ExtensionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class EncryptedPacketHandler extends PacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(EncryptedPacketHandler.class);

    /**
     * Fires when a packet is received.
     */
    private final Observable<BufferChangeListener> packetReceivedObservable;
    private final HMessage.Direction direction;
    private volatile boolean isTempBlocked;
    private volatile boolean isEncryptedStream;
    private volatile List<Byte> tempEncryptedBuffer;

    private RC4Cipher encryptCipher;
    private RC4Cipher decryptCipher;

    protected EncryptedPacketHandler(ExtensionHandler extensionHandler, Observable<TrafficListener>[] trafficObservables, HMessage.Direction direction) {
        super(extensionHandler, trafficObservables);

        this.packetReceivedObservable = new Observable<>(BufferChangeListener::onPacket);
        this.direction = direction;
        this.isTempBlocked = false;
        this.isEncryptedStream = false;
        this.tempEncryptedBuffer = new ArrayList<>();
    }

    public boolean isBlocked() {
        return isTempBlocked;
    }

    public void setEncryptedStream() {
        isEncryptedStream = true;
    }

    public boolean isEncryptedStream() {
        return isEncryptedStream;
    }

    public HMessage.Direction getDirection() {
        return direction;
    }

    public Observable<BufferChangeListener> getPacketReceivedObservable() {
        return packetReceivedObservable;
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        packetReceivedObservable.fireEvent();

        if (!isEncryptedStream()) {
            writeBuffer(buffer);
        } else if (!HConnection.DECRYPTPACKETS) {
            writeOut(buffer);
        } else if (decryptCipher == null) {
            for (int i = 0; i < buffer.length; i++) {
                tempEncryptedBuffer.add(buffer[i]);
            }
        } else {
            writeBuffer(buffer);
        }
    }

    protected byte[] encrypt(byte[] buffer) {
        return encryptCipher.cipher(buffer);
    }

    protected byte[] decrypt(byte[] buffer) {
        return decryptCipher.decipher(buffer);
    }

    protected abstract void writeOut(byte[] buffer) throws IOException;

    protected abstract void writeBuffer(byte[] buffer) throws IOException;

    protected abstract void flush() throws IOException;

    public void block() {
        isTempBlocked = true;
    }

    public void unblock() {
        try {
            flush();
        } catch (IOException e) {
            logger.error("Failed to flush buffer after unblocking packet handler", e);
        }

        isTempBlocked = false;
    }

    public boolean isCiphersSet() {
        return encryptCipher != null && decryptCipher != null;
    }

    public void setRc4(RC4Cipher rc4) {
        this.decryptCipher = rc4.deepCopy();
        this.encryptCipher = rc4.deepCopy();

        final byte[] buffer = new byte[tempEncryptedBuffer.size()];

        for (int i = 0; i < tempEncryptedBuffer.size(); i++) {
            buffer[i] = tempEncryptedBuffer.get(i);
        }

        try {
            // Write out all captured packets.
            act(buffer);
        } catch (IOException e) {
            logger.error("Failed to write out captured encrypted buffer", e);
        }

        this.tempEncryptedBuffer.clear();
        this.tempEncryptedBuffer = null;
    }

    public List<Byte> getEncryptedBuffer() {
        return tempEncryptedBuffer;
    }

}
