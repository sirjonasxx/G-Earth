package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.connection.*;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.NitroProxyProvider;
import gearth.services.internal_extensions.uilogger.hexdumper.Hexdump;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint(value = "/")
public class NitroWebsocketClient implements NitroSession {

    private final HProxySetter proxySetter;
    private final HStateSetter stateSetter;
    private final HConnection connection;
    private final NitroProxyProvider proxyProvider;
    private final NitroWebsocketServer server;
    private final NitroPacketHandler packetHandler;

    private Session activeSession = null;
    private HProxy proxy = null;

    public NitroWebsocketClient(HProxySetter proxySetter, HStateSetter stateSetter, HConnection connection, NitroProxyProvider proxyProvider) {
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.connection = connection;
        this.proxyProvider = proxyProvider;
        this.server = new NitroWebsocketServer(connection, this);
        this.packetHandler = new NitroPacketHandler(HMessage.Direction.TOSERVER, server, connection.getExtensionHandler(), connection.getTrafficObservables());
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        activeSession = session;
        activeSession.setMaxBinaryMessageBufferSize(NitroConstants.WEBSOCKET_BUFFER_SIZE);

        server.connect(proxyProvider.getOriginalWebsocketUrl());

        proxy = new HProxy(HClient.NITRO, "", "", -1, -1, "");
        proxy.verifyProxy(
                this.server.getPacketHandler(),
                this.packetHandler,
                NitroConstants.WEBSOCKET_REVISION,
                "HTML5" // TODO: What is its purpose?
        );

        proxySetter.setProxy(proxy);
        stateSetter.setState(HState.CONNECTED);
    }

    @OnMessage
    public void onMessage(byte[] b, Session session) throws IOException {
        System.out.printf("onMessage (%d)%n", b.length);
        System.out.println(session);
        System.out.println(Hexdump.hexdump(b));

        packetHandler.act(b);
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        activeSession = null;
    }

    @OnError
    public void onError(Session session, Throwable throwable) {

    }

    @Override
    public Session getSession() {
        return activeSession;
    }
}
