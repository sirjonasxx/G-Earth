package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.PayloadBuffer;
import gearth.services.extension_handler.ExtensionHandler;
import gearth.services.extension_handler.OnHMessageHandled;

import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;

public class NitroPacketHandler extends PacketHandler {

    private final HMessage.Direction direction;
    private final NitroSession session;
    private final PayloadBuffer payloadBuffer;
    private final Object payloadLock;

    protected NitroPacketHandler(HMessage.Direction direction, NitroSession session, ExtensionHandler extensionHandler, Object[] trafficObservables) {
        super(extensionHandler, trafficObservables);
        this.direction = direction;
        this.session = session;
        this.payloadBuffer = new PayloadBuffer();
        this.payloadLock = new Object();
    }

    @Override
    public boolean sendToStream(byte[] buffer) {
        final Session localSession = session.getSession();

        if (localSession == null) {
            return false;
        }

        // Required to prevent garbage buffer within the UI logger.
        if (direction == HMessage.Direction.TOSERVER) {
            buffer = buffer.clone();
        }

        localSession.getAsyncRemote().sendBinary(ByteBuffer.wrap(buffer));
        return true;
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        payloadBuffer.push(buffer);

        synchronized (payloadLock) {
            for (HPacket packet : payloadBuffer.receive()) {
                HMessage hMessage = new HMessage(packet, direction, currentIndex);

                OnHMessageHandled afterExtensionIntercept = hMessage1 -> {
                    notifyListeners(2, hMessage1);

                    if (!hMessage1.isBlocked())	{
                        sendToStream(hMessage1.getPacket().toBytes());
                    }
                };

                notifyListeners(0, hMessage);
                notifyListeners(1, hMessage);
                extensionHandler.handle(hMessage, afterExtensionIntercept);

                currentIndex++;
            }
        }
    }
}
