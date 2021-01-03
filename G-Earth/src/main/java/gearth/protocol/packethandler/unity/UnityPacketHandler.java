package gearth.protocol.packethandler.unity;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.packethandler.ByteArrayUtils;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.PayloadBuffer;
import gearth.services.extensionhandler.ExtensionHandler;
import gearth.services.extensionhandler.OnHMessageHandled;

import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;

public class UnityPacketHandler extends PacketHandler {

    private final Session session;
    private final HMessage.Direction direction;

    public UnityPacketHandler(ExtensionHandler extensionHandler, Object[] trafficObservables, Session session, HMessage.Direction direction) {
        super(extensionHandler, trafficObservables);
        this.session = session;
        this.direction = direction;
    }

    @Override
    public void sendToStream(byte[] buffer) {
        byte[] prefix = new byte[]{(direction == HMessage.Direction.TOCLIENT ? ((byte)0) : ((byte)1))};
        byte[] combined = ByteArrayUtils.combineByteArrays(prefix, buffer);

        session.getAsyncRemote().sendBinary(ByteBuffer.wrap(combined));
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        HMessage hMessage = new HMessage(new HPacket(buffer), direction, currentIndex);

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
