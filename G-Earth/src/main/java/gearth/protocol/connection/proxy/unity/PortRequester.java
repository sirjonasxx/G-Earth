package gearth.protocol.connection.proxy.unity;

import javafx.beans.InvalidationListener;
import org.eclipse.jetty.websocket.jsr356.annotations.JsrParamIdText;

import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

// tells which port the packethandler is running on
// server gets closed afterwards
@ServerEndpoint(value = "/portrequest")
public class PortRequester {

    private final int packetHandlerPort;

    public PortRequester(int port) {
        this.packetHandlerPort = port;
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        session.getBasicRemote().sendText("port " + packetHandlerPort);
    }
}
