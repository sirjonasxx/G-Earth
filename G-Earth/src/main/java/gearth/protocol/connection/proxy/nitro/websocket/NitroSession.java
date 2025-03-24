package gearth.protocol.connection.proxy.nitro.websocket;

import java.io.IOException;

public interface NitroSession {

    void send(byte[] buffer) throws IOException;

}
