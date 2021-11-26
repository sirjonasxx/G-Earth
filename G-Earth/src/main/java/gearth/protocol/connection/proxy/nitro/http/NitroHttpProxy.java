package gearth.protocol.connection.proxy.nitro.http;

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

    private HttpProxyServer proxyServer = null;

    public NitroHttpProxy() {
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
        return this.osFunctions.registerSystemProxy("127.0.0.1", 9090);
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
                    .withPort(9090)
                    .withManInTheMiddle(new CertificateSniffingMitmManager(authority))
                    // TODO: Replace lambda with some class
                    .withFiltersSource(new NitroHttpProxyFilterSource((configUrl, websocketUrl) -> {
                        System.out.printf("Found %s at %s%n", websocketUrl, configUrl);

                        return "wss://127.0.0.1:2096";
                    }))
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

    public void stop() {
        if (!unregisterProxy()) {
            System.out.println("Failed to unregister system proxy, please check manually");
        }

        if (proxyServer == null) {
            return;
        }

        proxyServer.stop();
        proxyServer = null;
    }
}
