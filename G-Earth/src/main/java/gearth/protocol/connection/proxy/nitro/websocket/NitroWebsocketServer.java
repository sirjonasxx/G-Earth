package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.packethandler.PacketHandler;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class NitroWebsocketServer implements NitroSession {

    private final PacketHandler packetHandler;
    private Session activeSession = null;

    public NitroWebsocketServer(HConnection connection, NitroWebsocketClient client) {
        this.packetHandler = new NitroPacketHandler(HMessage.Direction.TOCLIENT, client, connection.getExtensionHandler(), connection.getTrafficObservables());
    }

    public void connect(String websocketUrl) throws IOException {
        try {
            ContainerProvider.getWebSocketContainer().connectToServer(this, URI.create(websocketUrl));
        } catch (DeploymentException e) {
            throw new IOException("Failed to deploy websocket client", e);
        }
    }

    @OnOpen
    public void onOpen(Session Session) {
        this.activeSession = Session;
        this.activeSession.setMaxBinaryMessageBufferSize(NitroConstants.WEBSOCKET_BUFFER_SIZE);
    }

    @OnMessage
    public void onMessage(byte[] b, Session session) throws IOException {
        //System.out.printf("onMessage (%d)%n", b.length);
        //System.out.println(session);
        //System.out.println(Hexdump.hexdump(b));

        packetHandler.act(b);
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        //System.out.println("closing websocket");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        //System.out.println("onError");
        //System.out.println(session);
        //System.out.println(throwable);
    }

    @Override
    public Session getSession() {
        return activeSession;
    }

    public PacketHandler getPacketHandler() {
        return packetHandler;
    }
}
