package gearth.protocol.packethandler.shockwave;

import gearth.encoding.HexEncoding;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.TrafficListener;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveBuffer;
import gearth.protocol.packethandler.shockwave.crypto.RC4Shockwave;
import gearth.services.extension_handler.ExtensionHandler;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class ShockwavePacketHandler extends PacketHandler {

    // The first 20 bytes of the artificialKey.
    public static final byte[] ARTIFICIAL_KEY = Hex.decode("14d288cdb0bc08c274809a7802962af98b41dec8");

    protected static final Logger logger = LoggerFactory.getLogger(ShockwavePacketHandler.class);

    private final HMessage.Direction direction;
    private final ShockwaveBuffer payloadBuffer;
    private final Object flushLock;

    protected final OutputStream outputStream;

    private boolean isEncrypted;
    private final RC4Shockwave decryptCipher;
    private final RC4Shockwave encryptCipher;

    ShockwavePacketHandler(HMessage.Direction direction, ShockwaveBuffer payloadBuffer, OutputStream outputStream, ExtensionHandler extensionHandler, Observable<TrafficListener>[] trafficObservables) {
        super(extensionHandler, trafficObservables);
        this.direction = direction;
        this.payloadBuffer = payloadBuffer;
        this.outputStream = outputStream;
        this.flushLock = new Object();
        this.isEncrypted = false;
        this.decryptCipher = new RC4Shockwave(0, ARTIFICIAL_KEY);
        this.encryptCipher = new RC4Shockwave(0, ARTIFICIAL_KEY);
    }

    protected void setEncrypted() {
        isEncrypted = true;
    }

    @Override
    public boolean sendToStream(byte[] buffer) {
        return sendToStream(buffer, isEncrypted);
    }

    private boolean sendToStream(byte[] buffer, boolean isEncrypted) {
        synchronized (sendLock) {
            try {
                if (!isEncrypted) {
                    outputStream.write(buffer);
                } else {
                    outputStream.write(HexEncoding.toHex(encryptCipher.crypt(buffer), true));
                }
                return true;
            } catch (IOException e) {
                logger.error("Failed to send packet to stream", e);
                return false;
            }
        }
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        if (!isEncrypted) {
            payloadBuffer.push(buffer);
        } else {
            payloadBuffer.push(decryptCipher.crypt(Hex.decode(buffer)));
        }

        flush();
    }

    public void flush() throws IOException {
        synchronized (flushLock) {
            final HPacket[] packets = payloadBuffer.receive();

            for (final HPacket packet : packets){
                packet.setIdentifierDirection(direction);

                final HMessage message = new HMessage(packet, direction, currentIndex);

                awaitListeners(message, x -> sendToStream(x.getPacket().toBytes()));

                currentIndex++;
            }
        }
    }
}
