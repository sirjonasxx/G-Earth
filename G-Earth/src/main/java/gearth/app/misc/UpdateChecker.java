package gearth.app.misc;

import gearth.app.GEarth;
import gearth.app.ui.titlebar.TitleBarAlert;
import gearth.app.ui.translations.LanguageBundle;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpdateChecker {

    private static final Logger logger = LoggerFactory.getLogger(UpdateChecker.class);

    public static void checkForUpdates() {
        new Thread(() -> {
            // Check official repository first.
            if (!GEarth.repository.equals(GEarth.OFFICIAL_REPOSITORY)) {
                if (checkRepository(GEarth.OFFICIAL_REPOSITORY)) {
                    return;
                }
            }

            // Check repository of the fork.
            checkRepository("G-Realm/G-Earth");
        }).start();
    }

    private static boolean checkRepository(String repository) {
        final String currentVersion = GEarth.version;
        final String latestReleaseApi = String.format("https://api.github.com/repos/%s/releases/latest", repository);
        final String latestRelease = String.format("https://github.com/%s/releases/latest", repository);

        try {
            JSONObject object = new JSONObject(IOUtils.toString(
                    new URL(latestReleaseApi).openStream(), StandardCharsets.UTF_8));

            String gitv = (String)object.get("tag_name");

            if (gitv.startsWith("v")) {
                gitv = gitv.substring(1);
            }

            if (new ComparableVersion(currentVersion).compareTo(new ComparableVersion(gitv)) < 0) {
                final String newVersion = gitv;

                Platform.runLater(() -> {
                    final String body = (String)object.get("body");
                    final boolean isForcedUpdate = body.contains("(!)");
                    final Alert alert = new Alert(isForcedUpdate ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
                    FlowPane fp = new FlowPane();
                    Label lbl = new Label(LanguageBundle.get("alert.outdated.content.newversion") + " ("+newVersion+")" + System.lineSeparator() + System.lineSeparator() + LanguageBundle.get("alert.outdated.content.update"));
                    Hyperlink link = new Hyperlink(latestRelease);
                    link.setPadding(Insets.EMPTY);
                    fp.getChildren().addAll(lbl, link);
                    link.setOnAction(event -> {
                        GEarth.main.getHostServices().showDocument(link.getText());
                        event.consume();
                    });

                    alert.setTitle(LanguageBundle.get("alert.outdated.title"));
                    alert.setHeaderText(null);
                    alert.setResizable(false);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.getDialogPane().setContent(fp);
                    if (isForcedUpdate) {
                        alert.setOnCloseRequest(event -> System.exit(0));
                    }
                    try {
                        TitleBarAlert.create(alert).showAlert();
                    } catch (IOException e) {
                        logger.error("Failed to show alert", e);
                    }
                });

                return true;
            }
        } catch (IOException e) {
            logger.error("Failed to check for updates", e);
        }

        return false;
    }

}
