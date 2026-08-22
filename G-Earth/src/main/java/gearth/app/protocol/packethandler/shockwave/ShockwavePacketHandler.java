package gearth.app.protocol.packethandler.shockwave;

import gearth.app.protocol.TrafficListener;
import gearth.app.protocol.packethandler.PacketHandler;
import gearth.app.services.extension_handler.ExtensionHandler;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class ShockwavePacketHandler extends PacketHandler {

    protected static final Logger logger = LoggerFactory.getLogger(ShockwavePacketHandler.class);

    private final ShockwavePacketModifier packetModifier;
    private final HMessage.Direction direction;
    private final HPacketFormat format;
    private final Object flushLock;

    protected final OutputStream outputStream;

    public ShockwavePacketHandler(ShockwavePacketModifier packetModifier, HMessage.Direction direction, OutputStream outputStream, ExtensionHandler extensionHandler, Observable<TrafficListener>[] trafficObservables) {
        super(extensionHandler, trafficObservables);
        this.packetModifier = packetModifier;
        this.direction = direction;
        this.format = direction == HMessage.Direction.TOSERVER ? HPacketFormat.WEDGIE_OUTGOING : HPacketFormat.WEDGIE_INCOMING;
        this.outputStream = outputStream;
        this.flushLock = new Object();
    }

    @Override
    public boolean sendToStream(byte[] buffer) {
        final byte[] message;

        try {
            message = this.direction == HMessage.Direction.TOSERVER
                    ? this.packetModifier.gearthToServer(buffer)
                    : this.packetModifier.gearthToClient(buffer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to modify gearthToClient Shockwave packet.", e);
        }

        synchronized (sendLock) {
            try {
                outputStream.write(message);
                return true;
            } catch (IOException e) {
                logger.error("Failed to send packet to stream", e);
                return false;
            }
        }
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        final byte[][] messages;

        try {
            messages = this.direction == HMessage.Direction.TOSERVER
                    ? this.packetModifier.clientToGearth(buffer)
                    : this.packetModifier.serverToGearth(buffer);
        } catch (Exception e) {
            throw new RuntimeException("Failed to modify Shockwave packets.", e);
        }

        for (byte[] message : messages) {
            handle(message);
        }
    }

    private void handle(byte[] messageBytes) {
        synchronized (flushLock) {
            final HPacket packet = format.createPacket(messageBytes);

            packet.setIdentifierDirection(direction);

            final HMessage message = new HMessage(packet, direction, currentIndex);

            awaitListeners(message, x -> sendToStream(x.getPacket().toBytes()));

            currentIndex++;
        }
    }
}
