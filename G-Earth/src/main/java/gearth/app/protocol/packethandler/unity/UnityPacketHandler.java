package gearth.app.protocol.packethandler.unity;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.app.protocol.TrafficListener;
import gearth.app.protocol.connection.proxy.http.WebSession;
import gearth.app.protocol.packethandler.ByteArrayUtils;
import gearth.app.protocol.packethandler.PacketHandler;
import gearth.app.services.extension_handler.ExtensionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class UnityPacketHandler extends PacketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(UnityPacketHandler.class);

    private static final byte[] DIR_TO_CLIENT = new byte[]{0};
    private static final byte[] DIR_TO_SERVER = new byte[]{1};

    private final WebSession session;
    private final HMessage.Direction direction;
    private final Object actLock;

    public UnityPacketHandler(ExtensionHandler extensionHandler, Observable<TrafficListener>[] trafficObservables, WebSession session, HMessage.Direction direction) {
        super(extensionHandler, trafficObservables);
        this.session = session;
        this.direction = direction;
        this.actLock = new Object();
    }

    @Override
    public boolean sendToStream(byte[] buffer) {
        final byte[] prefix = (direction == HMessage.Direction.TOCLIENT ? DIR_TO_CLIENT : DIR_TO_SERVER);
        final byte[] combined = ByteArrayUtils.combineByteArrays(prefix, buffer);

        try {
            if (!session.send(combined)) {
                LOG.warn("Discarding {} bytes because the session for direction {} was closed", buffer.length, this.direction);
                return false;
            }

            return true;
        } catch (IOException e) {
            LOG.error("Error sending packet to unity direction {}", this.direction, e);
            return false;
        }
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        synchronized (actLock) {
            final HMessage hMessage = new HMessage(new HPacket(buffer), direction, currentIndex);

            awaitListeners(hMessage, hMessage1 -> sendToStream(hMessage1.getPacket().toBytes()));
            currentIndex++;
        }
    }
}
