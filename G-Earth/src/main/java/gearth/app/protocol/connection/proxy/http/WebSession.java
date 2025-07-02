package gearth.app.protocol.connection.proxy.http;

import java.io.IOException;

public interface WebSession {

    boolean send(byte[] buffer) throws IOException;

}
