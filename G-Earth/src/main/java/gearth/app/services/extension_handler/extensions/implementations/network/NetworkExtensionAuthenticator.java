package gearth.app.services.extension_handler.extensions.implementations.network;

import gearth.app.misc.ConfirmationDialog;
import gearth.app.ui.titlebar.TitleBarAlert;
import gearth.app.ui.translations.LanguageBundle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Jonas on 16/10/18.
 */
public final class NetworkExtensionAuthenticator {

    private static final Map<String, String> COOKIES = new HashMap<>();
    private static final Set<String> PERSISTENT_COOKIES = new HashSet<>();

    private static volatile boolean rememberOption = false;

    public static boolean evaluate(NetworkExtensionClient extension) {

        final String cookie = extension.getCookie();

        if (cookie != null && PERSISTENT_COOKIES.contains(cookie))
            return true;

        return extension.isInstalledExtension()
                ? claimSession(extension.getFileName(), cookie)
                : askForPermission(extension);
    }

    /**
     * Authenticate an extension and remove the cookie
     *
     * @return {@code true} if the extension is authenticated.
     */
    private static boolean claimSession(String filename, String cookie) {
        if (COOKIES.containsKey(filename) && COOKIES.get(filename).equals(cookie)) {
            COOKIES.remove(filename);
            return true;
        }
        return false;
    }

    /**
     * For not yet installed extensions, open a confirmation dialog.
     *
     * @param extension the {@link NetworkExtensionClient extension} to ask permission for.
     *
     * @return {@code true} if permission is granted, {@code false} if not.
     */
    private static boolean askForPermission(NetworkExtensionClient extension) {
        boolean[] allowConnection = {true};

        final String connectExtensionKey = "allow_extension_connection";

        if (ConfirmationDialog.showDialog(connectExtensionKey)) {

            final CountDownLatch countDownLatch = new CountDownLatch(0);

            Platform.runLater(() -> {
                Alert alert = ConfirmationDialog.createAlertWithOptOut(Alert.AlertType.WARNING, connectExtensionKey
                        , LanguageBundle.get("alert.confirmation.windowtitle"), null,
                        "", LanguageBundle.get("alert.confirmation.button.remember"),
                        ButtonType.YES, ButtonType.NO
                );

                alert.getDialogPane().setContent(new Label(String.format(LanguageBundle.get("alert.extconnection.content"), extension.getTitle()).replaceAll("\\\\n", System.lineSeparator())));

                try {
                    if (!(TitleBarAlert.create(alert).showAlertAndWait()
                            .filter(t -> t == ButtonType.YES).isPresent())) {
                        allowConnection[0] = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                countDownLatch.countDown();
                if (!ConfirmationDialog.showDialog(connectExtensionKey)) {
                    rememberOption = allowConnection[0];
                }
            });

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return allowConnection[0];
        }

        return rememberOption;
    }

    public static String generateCookieForExtension(String filename) {
        final String cookie = generateRandomCookie();
        COOKIES.put(filename, cookie);
        return cookie;
    }

    public static String generatePermanentCookie() {
        final String cookie = generateRandomCookie();
        PERSISTENT_COOKIES.add(cookie);
        return cookie;
    }

    private static String generateRandomCookie() {
        final StringBuilder builder = new StringBuilder();
        final Random r = new Random();
        for (int i = 0; i < 40; i++)
            builder.append(r.nextInt(40));
        return builder.toString();
    }
}
