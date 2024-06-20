package gearth.protocol.connection.proxy.nitro.http;

import gearth.misc.ConfirmationDialog;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctions;
import gearth.protocol.connection.proxy.nitro.os.NitroOsFunctionsFactory;
import gearth.ui.titlebar.TitleBarController;
import gearth.ui.translations.LanguageBundle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

public class NitroHttpProxy {

    private static final String ADMIN_WARNING_KEY = "admin_warning_dialog";
    private static final AtomicBoolean SHUTDOWN_HOOK = new AtomicBoolean();

    private final NitroOsFunctions osFunctions;
    private final NitroHttpProxyServerCallback serverCallback;
    private final NitroCertificateSniffingManager certificateManager;

    private HttpProxyServer proxyServer = null;

    public NitroHttpProxy(NitroHttpProxyServerCallback serverCallback, NitroCertificateSniffingManager certificateManager) {
        this.serverCallback = serverCallback;
        this.certificateManager = certificateManager;
        this.osFunctions = NitroOsFunctionsFactory.create();
    }

    private boolean initializeCertificate() {
        final File certificate = this.certificateManager.getAuthority().aliasFile(".pem");

        // All good if certificate is already trusted.
        if (this.osFunctions.isRootCertificateTrusted(certificate)) {
            return true;
        }

        // Let the user know about admin permissions.
        final Semaphore waitForDialog = new Semaphore(0);
        final AtomicBoolean shouldInstall = new AtomicBoolean();

        Platform.runLater(() -> {
            Alert alert = ConfirmationDialog.createAlertWithOptOut(Alert.AlertType.WARNING, ADMIN_WARNING_KEY,
                    LanguageBundle.get("alert.rootcertificate.title"), null,
                    "", LanguageBundle.get("alert.rootcertificate.remember"),
                    ButtonType.YES, ButtonType.NO
            );

            alert.getDialogPane().setContent(new Label(LanguageBundle.get("alert.rootcertificate.content").replaceAll("\\\\n", System.lineSeparator())));

            try {
                shouldInstall.set(TitleBarController.create(alert).showAlertAndWait()
                        .filter(t -> t == ButtonType.YES).isPresent());
            } catch (IOException e) {
                e.printStackTrace();
            }
            waitForDialog.release();
        });

        // Wait for dialog choice.
        try {
            waitForDialog.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        // User opted out.
        if (!shouldInstall.get()) {
            return false;
        }

        return this.osFunctions.installRootCertificate(this.certificateManager.getAuthority().aliasFile(".pem"));
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

        proxyServer = DefaultHttpProxyServer.bootstrap()
                .withPort(NitroConstants.HTTP_PORT)
                .withManInTheMiddle(this.certificateManager)
                .withFiltersSource(new NitroHttpProxyFilterSource(serverCallback))
                .withTransparent(true)
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
