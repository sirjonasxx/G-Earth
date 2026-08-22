package gearth.app.protocol.connection.proxy.nitro.os;

import java.io.File;

public interface NitroOsFunctions {

    boolean isRootCertificateTrusted(File certificate);

    boolean installRootCertificate(File certificate);

    boolean registerSystemProxy(String host, int port);

    boolean unregisterSystemProxy();

}
