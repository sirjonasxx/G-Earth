package gearth.protocol.connection.proxy.http;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.proxy.ProxyConfig;
import com.github.monkeywie.proxyee.proxy.ProxyType;
import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import gearth.misc.ConfirmationDialog;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.SocksConfiguration;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctions;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctionsFactory;
import gearth.ui.titlebar.TitleBarAlert;
import gearth.ui.translations.LanguageBundle;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.net.ServerSocket;
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpProxyManager {

    private static final Logger log = LoggerFactory.getLogger(HttpProxyManager.class);

    private static final String ADMIN_WARNING_KEY = "admin_warning_dialog";
    private static final AtomicBoolean SHUTDOWN_HOOK = new AtomicBoolean();

    private final HttpProxyCertificateFactory certificateFactory;
    private final NitroOsFunctions osFunctions;

    private HttpProxyServer proxyServer = null;

    public HttpProxyManager() {
        this.certificateFactory = new HttpProxyCertificateFactory();
        this.osFunctions = NitroOsFunctionsFactory.create();
    }

    private boolean initializeCertificate() {
        if (!this.certificateFactory.loadOrCreate()) {
            return false;
        }

        final File certificate = this.certificateFactory.getCaCertFile();

        // All good if certificate is already trusted.
        if (this.osFunctions.isRootCertificateTrusted(certificate)) {
            return true;
        }

        // Let the user know about admin permissions.
        final Semaphore waitForDialog = new Semaphore(0);
        final AtomicBoolean shouldInstall = new AtomicBoolean();

        Platform.runLater(() -> {
            try {
                Alert alert = ConfirmationDialog.createAlertWithOptOut(Alert.AlertType.WARNING, ADMIN_WARNING_KEY,
                        LanguageBundle.get("alert.rootcertificate.title"), null,
                        "", LanguageBundle.get("alert.rootcertificate.remember"),
                        ButtonType.YES, ButtonType.NO
                );

                alert.getDialogPane().setContent(new Label(LanguageBundle.get("alert.rootcertificate.content").replaceAll("\\\\n", System.lineSeparator())));

                log.debug("Showing certificate install dialog");

                shouldInstall.set(TitleBarAlert.create(alert).showAlertAndWait()
                        .filter(t -> t == ButtonType.YES).isPresent());
            } catch (Exception e) {
                log.error("Failed to show install dialog", e);
            } finally {
                waitForDialog.release();
            }
        });

        // Wait for dialog choice.
        try {
            waitForDialog.acquire();
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for user input", e);
            return false;
        }

        // User opted out.
        if (!shouldInstall.get()) {
            return false;
        }

        return this.osFunctions.installRootCertificate(this.certificateFactory.getCaCertFile());
    }

    /**
     * Register HTTP(s) proxy on the system.
     */
    private boolean registerProxy(int port) {
        return this.osFunctions.registerSystemProxy("127.0.0.1", port);
    }

    /**
     * Unregister HTTP(s) proxy from system.
     */
    private boolean unregisterProxy() {
        return this.osFunctions.unregisterSystemProxy();
    }

    public boolean start(HttpProxyInterceptInitializer proxyInterceptInitializer) {
        setupShutdownHook();

        if (!initializeCertificate()) {
            log.error("Failed to initialize certificate");
            return false;
        }

        final HttpProxyServerConfig config = new HttpProxyServerConfig();

        config.setHandleSsl(true);
        config.setWsDecoderConfig(WebSocketDecoderConfig.newBuilder()
                .allowExtensions(true)
                .maxFramePayloadLength(NitroConstants.WEBSOCKET_BUFFER_SIZE)
                .build());

        proxyServer = new HttpProxyServer()
                .serverConfig(config)
                .caCertFactory(this.certificateFactory)
                .proxyInterceptInitializer(proxyInterceptInitializer);

        final SocksConfiguration socks = ProxyProviderFactory.getSocksConfig();

        if (socks != null && socks.useSocks()) {
            proxyServer.proxyConfig(new ProxyConfig(ProxyType.SOCKS5,
                    socks.getSocksHost(),
                    socks.getSocksPort()));
        }

        final int httpPort = getFreePort();

        if (httpPort == -1) {
            log.error("Failed to get free port for http proxy");
            return false;
        }

        log.info("Starting http proxy on port {}", httpPort);
        proxyServer.startAsync(httpPort);

        // Hack to swap the SSL context.
        // Need to set this after proxyServer is started because starting it will override the configured SSL context.
        try {
            Security.addProvider(new BouncyCastleProvider());

            config.setClientSslCtx(SslContextBuilder
                    .forClient()
                    .sslContextProvider(new BouncyCastleJsseProvider())
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .protocols("TLSv1.3", "TLSv1.2")
                    .ciphers(new HashSet<>(Arrays.asList(NitroConstants.CIPHER_SUITES)))
                    .build());
        } catch (SSLException e) {
            proxyServer.close();

            log.error("Failed to create proxy SSL context", e);
            return false;
        }

        if (!registerProxy(httpPort)) {
            proxyServer.close();

            log.error("Failed to register system proxy");
            return false;
        }

        return true;
    }

    public void pause() {
        if (!unregisterProxy()) {
            log.error("Failed to unregister system proxy, please check manually");
        }
    }

    public void stop() {
        pause();

        if (proxyServer == null) {
            return;
        }

        proxyServer.close();
        proxyServer = null;
    }

    private static int getFreePort() {
        ServerSocket socket = null;

        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (Exception e) {
            log.error("Failed to get free port", e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    log.error("Failed to close socket", e);
                }
            }
        }

        return -1;
    }

    /**
     * Ensure the system proxy is removed when G-Earth exits.
     * Otherwise, users might complain that their browsers / discord stop working when closing G-Earth incorrectly.
     */
    private static void setupShutdownHook() {
        if (SHUTDOWN_HOOK.get()) {
            return;
        }

        if (SHUTDOWN_HOOK.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> NitroOsFunctionsFactory.create().unregisterSystemProxy()));
        }
    }
}
