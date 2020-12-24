package gearth.protocol.connection.proxy;

import java.io.IOException;

public interface ProxyProvider {

    void start() throws IOException;
    void abort();

}
