package gearth.protocol.connection.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;

public interface SocksConfiguration {
    boolean useSocks();

    int getSocksPort();
    String getSocksHost();
    boolean onlyUseIfNeeded();


    default Socket createSocket() throws SocketException {
        Proxy socks = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(getSocksHost(), getSocksPort()));
        Socket server = new Socket(socks);
        server.setSoTimeout(5000);

        return server;
    }
}
