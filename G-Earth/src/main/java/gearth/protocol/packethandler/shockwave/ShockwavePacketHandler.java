package gearth.protocol.packethandler.shockwave;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.protocol.TrafficListener;
import gearth.protocol.crypto.RC4Cipher;
import gearth.protocol.packethandler.EncryptedPacketHandler;
import gearth.protocol.packethandler.PayloadBuffer;
import gearth.services.extension_handler.ExtensionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ShockwavePacketHandler extends EncryptedPacketHandler {

    protected static final Logger logger = LoggerFactory.getLogger(ShockwavePacketHandler.class);

    private final HMessage.Direction direction;
    private final HPacketFormat format;
    private final PayloadBuffer payloadBuffer;
    private final Object flushLock;

    protected final OutputStream outputStream;

    ShockwavePacketHandler(HMessage.Direction direction, PayloadBuffer payloadBuffer, OutputStream outputStream, ExtensionHandler extensionHandler, Observable<TrafficListener>[] trafficObservables) {
        super(extensionHandler, trafficObservables, direction);
        this.direction = direction;
        this.format = direction == HMessage.Direction.TOSERVER ? HPacketFormat.WEDGIE_OUTGOING : HPacketFormat.WEDGIE_INCOMING;
        this.payloadBuffer = payloadBuffer;
        this.outputStream = outputStream;
        this.flushLock = new Object();
    }

    @Override
    public boolean sendToStream(byte[] buffer) {
        synchronized (sendLock) {
            try {
                outputStream.write(buffer);
                return true;
            } catch (IOException e) {
                logger.error("Failed to send packet to stream", e);
                return false;
            }
        }
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        super.act(buffer);

        if (!isBlocked()) {
            flush();
        }
    }

    @Override
    protected void writeOut(byte[] buffer) throws IOException {
        synchronized (sendLock) {
            outputStream.write(buffer);
        }
    }

    @Override
    protected void writeBuffer(byte[] buffer) {
        payloadBuffer.push(buffer);
    }

    @Override
    public void setRc4(RC4Cipher rc4) {
        payloadBuffer.setCipher(rc4);
        super.setRc4(rc4);
    }

    public void flush() throws IOException {
        synchronized (flushLock) {
            final byte[][] packets = payloadBuffer.receive();

            for (final byte[] rawPacket : packets) {
                final HPacket packet = isEncryptedStream()
                        ? format.createPacket(decrypt(rawPacket))
                        : format.createPacket(rawPacket);

                packet.setIdentifierDirection(direction);

                final HMessage message = new HMessage(packet, direction, currentIndex);

                awaitListeners(message, x -> sendToStream(x.getPacket().toBytes()));

                currentIndex++;
            }
        }
    }
}
