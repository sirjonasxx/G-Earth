package gearth.protocol.packethandler.shockwave;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveBuffer;
import gearth.services.extension_handler.ExtensionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ShockwavePacketHandler extends PacketHandler {

    protected static final Logger logger = LoggerFactory.getLogger(ShockwavePacketHandler.class);

    private final HMessage.Direction direction;
    private final ShockwaveBuffer payloadBuffer;
    private final Object flushLock;

    protected final OutputStream outputStream;

    ShockwavePacketHandler(HMessage.Direction direction, ShockwaveBuffer payloadBuffer, OutputStream outputStream, ExtensionHandler extensionHandler, Object[] trafficObservables) {
        super(extensionHandler, trafficObservables);
        this.direction = direction;
        this.payloadBuffer = payloadBuffer;
        this.outputStream = outputStream;
        this.flushLock = new Object();
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        logger.info("Direction {} Received {} bytes", this.direction, buffer.length);

        payloadBuffer.push(buffer);

        flush();
    }

    public void flush() throws IOException {
        synchronized (flushLock) {
            final HPacket[] packets = payloadBuffer.receive();

            for (final HPacket packet : packets){
                final HMessage message = new HMessage(packet, direction, currentIndex);

                awaitListeners(message, x -> sendToStream(x.getPacket().toBytes()));

                currentIndex++;
            }
        }
    }
}
