package gearth.services.extension_handler.extensions.implementations.network.authentication;

import gearth.GEarth;
import gearth.misc.ConfirmationDialog;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtension;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.io.IOException;
import java.util.*;

/**
 * Created by Jonas on 16/10/18.
 */
public class Authenticator {

    private static Map<String, String> cookies = new HashMap<>();
    private static Set<String> perma_cookies = new HashSet<>();

    public static String generateCookieForExtension(String filename) {
        String cookie = getRandomCookie();
        cookies.put(filename, cookie);
        return cookie;
    }
    public static String generatePermanentCookie() {
        String cookie = getRandomCookie();
        perma_cookies.add(cookie);
        return cookie;
    }

    public static boolean evaluate(NetworkExtension extension) {
        if (extension.getCookie() != null && perma_cookies.contains(extension.getCookie())) {
            return true;
        }

        if (extension.isInstalledExtension()) {
            return claimSession(extension.getFileName(), extension.getCookie());
        }
        else {
            return askForPermission(extension);
        }
    }

    /**
     * authenticator: authenticate an extension and remove the cookie
     * @param filename
     * @param cookie
     * @return if the extension is authenticated
     */
    private static boolean claimSession(String filename, String cookie) {
        if (cookies.containsKey(filename) && cookies.get(filename).equals(cookie)) {
            cookies.remove(filename);
            return true;
        }
        return false;
    }

    private static volatile boolean rememberOption = false;
    //for not-installed extensions, popup a dialog
    private static boolean askForPermission(NetworkExtension extension) {
        boolean[] allowConnection = {true};

        final String connectExtensionKey = "allow_extension_connection";

        if (ConfirmationDialog.showDialog(connectExtensionKey)) {
            boolean[] done = {false};
            Platform.runLater(() -> {
                Alert alert = ConfirmationDialog.createAlertWithOptOut(Alert.AlertType.WARNING, connectExtensionKey
                        , GEarth.translation.getString("alert.confirmation.windowtitle"), null,
                        "", GEarth.translation.getString("alert.confirmation.button.remember"),
                        ButtonType.YES, ButtonType.NO
                );

                alert.getDialogPane().setContent(new Label(String.format(GEarth.translation.getString("alert.extconnection.content"), extension.getTitle()).replaceAll("\\\\n", System.lineSeparator())));

                try {
                    if (!(TitleBarController.create(alert).showAlertAndWait()
                            .filter(t -> t == ButtonType.YES).isPresent())) {
                        allowConnection[0] = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                done[0] = true;
                if (!ConfirmationDialog.showDialog(connectExtensionKey)) {
                    rememberOption = allowConnection[0];
                }
            });

            while (!done[0]) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return allowConnection[0];
        }

        return rememberOption;
    }

    private static String getRandomCookie() {
        StringBuilder builder = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 40; i++) {
            builder.append(r.nextInt(40));
        }

        return builder.toString();
    }
}
