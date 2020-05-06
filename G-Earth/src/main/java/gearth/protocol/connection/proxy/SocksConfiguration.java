package gearth.protocol.connection.proxy;

public interface SocksConfiguration {
    boolean useSocks();

    int getSocksPort();
    String getSocksHost();
    boolean dontUseFirstTime();
}
