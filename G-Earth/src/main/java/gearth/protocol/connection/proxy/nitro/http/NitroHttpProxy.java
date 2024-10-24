package gearth.protocol.connection.proxy.nitro.http;

import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import gearth.misc.ConfirmationDialog;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctions;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctionsFactory;
import gearth.ui.titlebar.TitleBarController;
import gearth.ui.translations.LanguageBundle;
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
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class NitroHttpProxy {

    private static final Logger log = LoggerFactory.getLogger(NitroHttpProxy.class);

    private static final String ADMIN_WARNING_KEY = "admin_warning_dialog";
    private static final AtomicBoolean SHUTDOWN_HOOK = new AtomicBoolean();

    private final NitroOsFunctions osFunctions;
    private final NitroHttpProxyServerCallback serverCallback;
    private final NitroCertificateFactory certificateFactory;

    private HttpProxyServer proxyServer = null;

    public NitroHttpProxy(NitroHttpProxyServerCallback serverCallback, NitroCertificateFactory certificateManager) {
        this.serverCallback = serverCallback;
        this.certificateFactory = certificateManager;
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

                shouldInstall.set(TitleBarController.create(alert).showAlertAndWait()
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
        setupShutdownHook();

        if (!initializeCertificate()) {
            log.error("Failed to initialize certificate");
            return false;
        }

        final HttpProxyServerConfig config = new HttpProxyServerConfig();

        config.setHandleSsl(true);

        proxyServer = new HttpProxyServer()
                .serverConfig(config)
                .caCertFactory(this.certificateFactory)
                .proxyInterceptInitializer(new NitroHttpProxyIntercept(serverCallback));

        proxyServer.startAsync(NitroConstants.HTTP_PORT);

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

            log.error("Failed to create nitro proxy SSL context", e);
            return false;
        }

        // Add config to factory so websocket server can use it as well.
        this.certificateFactory.setServerConfig(config);

        if (!registerProxy()) {
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
