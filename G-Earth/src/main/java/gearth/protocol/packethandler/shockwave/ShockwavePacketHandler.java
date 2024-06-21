package gearth.protocol.packethandler.shockwave;

import gearth.protocol.HMessage;
import gearth.protocol.format.shockwave.ShockMessage;
import gearth.protocol.format.shockwave.ShockPacket;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveBuffer;
import gearth.services.extension_handler.ExtensionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ShockwavePacketHandler extends PacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ShockwavePacketHandler.class);

    private final HMessage.Direction direction;
    private final ShockwaveBuffer payloadBuffer;
    private final OutputStream outputStream;
    private final Object flushLock;

    ShockwavePacketHandler(HMessage.Direction direction, ShockwaveBuffer payloadBuffer, OutputStream outputStream, ExtensionHandler extensionHandler, Object[] trafficObservables) {
        super(extensionHandler, trafficObservables);
        this.direction = direction;
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
                logger.error("Error while sending packet to stream.", e);
                return false;
            }
        }
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        payloadBuffer.push(buffer);

        flush();
    }

    public void flush() throws IOException {
        synchronized (flushLock) {
            final ShockPacket[] packets = payloadBuffer.receive();

            for (final ShockPacket packet : packets){
                final ShockMessage message = new ShockMessage(packet, direction, currentIndex);

                awaitListeners(message, x -> sendToStream(x.getPacket().toBytes()));

                currentIndex++;
            }
        }
    }
}
