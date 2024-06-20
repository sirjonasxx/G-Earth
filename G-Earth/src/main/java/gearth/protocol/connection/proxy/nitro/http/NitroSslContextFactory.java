package gearth.protocol.connection.proxy.nitro.http;

import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.net.ssl.SSLEngine;

public class NitroSslContextFactory extends SslContextFactory.Server {

    private final NitroCertificateSniffingManager certificateManager;

    public NitroSslContextFactory(NitroCertificateSniffingManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    @Override
    public SSLEngine newSSLEngine(String host, int port) {
        System.out.printf("[NitroSslContextFactory] Creating SSLEngine for %s:%d%n", host, port);
        return certificateManager.websocketSslEngine(host);
    }
}
