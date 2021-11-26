package gearth.protocol.connection.proxy.nitro.http;

import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctions;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctionsFactory;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.CertificateSniffingMitmManager;
import org.littleshoot.proxy.mitm.RootCertificateException;

public class NitroHttpProxy {

    private final Authority authority;
    private final NitroOsFunctions osFunctions;
    private final NitroHttpProxyServerCallback serverCallback;

    private HttpProxyServer proxyServer = null;

    public NitroHttpProxy(NitroHttpProxyServerCallback serverCallback) {
        this.serverCallback = serverCallback;
        this.authority = new NitroAuthority();
        this.osFunctions = NitroOsFunctionsFactory.create();
    }

    private boolean initializeCertificate() {
        return this.osFunctions.installRootCertificate(this.authority.aliasFile(".pem"));
    }

    /**
     * Register HTTP(s) proxy on the system.
     */
    private boolean registerProxy() {
        return this.osFunctions.registerSystemProxy("127.0.0.1", NitroConstants.HTTP_PORT);
    }

    /**
     * Unregister HTTP(s) proxy from system.
     */
    private boolean unregisterProxy() {
        return this.osFunctions.unregisterSystemProxy();
    }

    public boolean start() {
        try {
            proxyServer = DefaultHttpProxyServer.bootstrap()
                    .withPort(NitroConstants.HTTP_PORT)
                    .withManInTheMiddle(new CertificateSniffingMitmManager(authority))
                    .withFiltersSource(new NitroHttpProxyFilterSource(serverCallback))
                    .start();

            if (!initializeCertificate()) {
                proxyServer.stop();

                System.out.println("Failed to initialize certificate");
                return false;
            }

            if (!registerProxy()) {
                proxyServer.stop();

                System.out.println("Failed to register certificate");
                return false;
            }

            return true;
        } catch (RootCertificateException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void pause() {
        if (!unregisterProxy()) {
            System.out.println("Failed to unregister system proxy, please check manually");
        }
    }

    public void stop() {
        pause();

        if (proxyServer == null) {
            return;
        }

        proxyServer.stop();
        proxyServer = null;
    }
}
