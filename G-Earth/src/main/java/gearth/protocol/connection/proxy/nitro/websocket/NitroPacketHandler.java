package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.packethandler.PacketHandler;
import gearth.services.extension_handler.ExtensionHandler;
import gearth.services.extension_handler.OnHMessageHandled;

import java.io.IOException;
import java.nio.ByteBuffer;

public class NitroPacketHandler extends PacketHandler {

    private final HMessage.Direction direction;
    private final NitroSession session;

    protected NitroPacketHandler(HMessage.Direction direction, NitroSession session, ExtensionHandler extensionHandler, Object[] trafficObservables) {
        super(extensionHandler, trafficObservables);
        this.direction = direction;
        this.session = session;
    }

    @Override
    public boolean sendToStream(byte[] buffer) {
        session.getSession().getAsyncRemote().sendBinary(ByteBuffer.wrap(buffer));
        return true;
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        HMessage hMessage = new HMessage(new HPacket(deepCopy(buffer)), direction, currentIndex);

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

    public static byte[] deepCopy(byte[] org) {
        if (org == null)
            return null;
        byte[] result = new byte[org.length];
        System.arraycopy(org, 0, result, 0, org.length);
        return result;
    }

}
