package gearth.misc;

import gearth.GEarth;
import gearth.ui.titlebar.TitleBarController;
import gearth.ui.translations.LanguageBundle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
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
        final String currentVersion = GEarth.version;
        final String latestReleaseApi = String.format("https://api.github.com/repos/%s/releases/latest", GEarth.repository);
        final String latestRelease = String.format("https://github.com/%s/releases/latest", GEarth.repository);

        new Thread(() -> {
            try {
                JSONObject object = new JSONObject(IOUtils.toString(
                        new URL(latestReleaseApi).openStream(), StandardCharsets.UTF_8));

                String gitv = (String)object.get("tag_name");
                if (new ComparableVersion(currentVersion).compareTo(new ComparableVersion(gitv)) < 0) {
                    Platform.runLater(() -> {
                        String body = (String)object.get("body");
                        boolean isForcedUpdate = body.contains("(!)");

                        Alert alert = new Alert(isForcedUpdate ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION, LanguageBundle.get("alert.outdated.title"), ButtonType.OK);

                        FlowPane fp = new FlowPane();
                        Label lbl = new Label(LanguageBundle.get("alert.outdated.content.newversion") + " ("+gitv+")" + System.lineSeparator()+ System.lineSeparator() + LanguageBundle.get("alert.outdated.content.update") + ":");
                        Hyperlink link = new Hyperlink(latestRelease);
                        fp.getChildren().addAll( lbl, link);
                        link.setOnAction(event -> {
                            GEarth.main.getHostServices().showDocument(link.getText());
                            event.consume();
                        });



                        WebView webView = new WebView();
                        webView.getEngine().loadContent(String.format("<html>%s (%s)<br><br>%s:<br><a href=\"%s\">%s</a></html>",
                                LanguageBundle.get("alert.outdated.content.newversion"),
                                gitv,
                                LanguageBundle.get("alert.outdated.content.update"),
                                latestRelease,
                                latestRelease));
                        webView.setPrefSize(500, 200);

                        alert.setResizable(false);
                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.getDialogPane().setContent(fp);
                        if (isForcedUpdate) {
                            alert.setOnCloseRequest(event -> System.exit(0));
                        }
                        try {
                            TitleBarController.create(alert).showAlert();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    });
                }

            } catch (IOException e) {
                logger.error("Failed to check for updates", e);
            }
        }).start();
    }

}
