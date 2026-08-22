package gearth.app.protocol.packethandler.nitro;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.app.protocol.TrafficListener;
import gearth.app.protocol.connection.proxy.http.WebSession;
import gearth.app.protocol.connection.proxy.nitro.websocket.NitroSessionProvider;
import gearth.app.protocol.packethandler.PacketHandler;
import gearth.app.protocol.packethandler.flash.FlashBuffer;
import gearth.app.services.extension_handler.ExtensionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NitroPacketHandler extends PacketHandler {

    private static final Logger logger = LoggerFactory.getLogger(NitroPacketHandler.class);

    private final HMessage.Direction direction;
    private final NitroSessionProvider session;
    private final FlashBuffer payloadBuffer;
    private final Object payloadLock;

    public NitroPacketHandler(HMessage.Direction direction, NitroSessionProvider session, ExtensionHandler extensionHandler, Observable<TrafficListener>[] trafficObservables) {
        super(extensionHandler, trafficObservables);
        this.direction = direction;
        this.session = session;
        this.payloadBuffer = new FlashBuffer();
        this.payloadLock = new Object();
    }

    @Override
    public boolean sendToStream(byte[] buffer) {
        final WebSession localSession = session.getSession();

        if (localSession == null) {
            logger.warn("Discarding {} bytes because the session for direction {} was null", buffer.length, this.direction);
            return false;
        }

        // Required to prevent garbage buffer within the UI logger.
        if (direction == HMessage.Direction.TOSERVER) {
            buffer = buffer.clone();
        }

        try {
            if (!localSession.send(buffer)) {
                logger.warn("Discarding {} bytes because the session for direction {} was closed", buffer.length, this.direction);
                return false;
            }
        } catch (IOException e) {
            logger.error("Error sending packet to nitro client", e);
            return false;
        }

        return true;
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        payloadBuffer.push(buffer);

        synchronized (payloadLock) {
            for (final byte[] packet : payloadBuffer.receive()) {
                HPacket hPacket = new HPacket(packet);
                HMessage hMessage = new HMessage(hPacket, direction, currentIndex);
                awaitListeners(hMessage, hMessage1 -> sendToStream(hMessage1.getPacket().toBytes()));
                currentIndex++;
            }
        }
    }
}
