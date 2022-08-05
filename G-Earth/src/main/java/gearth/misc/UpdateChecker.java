package gearth.misc;

import gearth.GEarth;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static gearth.GEarth.gitApi;
import static gearth.GEarth.version;

public class UpdateChecker {

    public static void checkForUpdates() {
        new Thread(() -> {
            try {
                JSONObject object = new JSONObject(IOUtils.toString(
                        new URL(gitApi).openStream(), StandardCharsets.UTF_8));

                String gitv = (String)object.get("tag_name");
                if (new ComparableVersion(version).compareTo(new ComparableVersion(gitv)) < 0) {
                    Platform.runLater(() -> {
                        String body = (String)object.get("body");
                        boolean isForcedUpdate = body.contains("(!)");

                        Alert alert = new Alert(isForcedUpdate ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION, GEarth.translation.getString("alert.outdated.title"), ButtonType.OK);

                        FlowPane fp = new FlowPane();
                        Label lbl = new Label(GEarth.translation.getString("alert.outdated.content.newversion") + " ("+gitv+")" + System.lineSeparator()+ System.lineSeparator() + GEarth.translation.getString("alert.outdated.content.update") + ":");
                        Hyperlink link = new Hyperlink("https://github.com/sirjonasxx/G-Earth/releases");
                        fp.getChildren().addAll( lbl, link);
                        link.setOnAction(event -> {
                            GEarth.main.getHostServices().showDocument(link.getText());
                            event.consume();
                        });



                        WebView webView = new WebView();
                        webView.getEngine().loadContent(String.format("<html>%s (%s)<br><br>%s:<br><a href=\"https://github.com/sirjonasxx/G-Earth/releases\">https://github.com/sirjonasxx/G-Earth/releases</a></html>", GEarth.translation.getString("alert.outdated.content.newversion"), gitv, GEarth.translation.getString("alert.outdated.content.update")));
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
//                e.printStackTrace();
            }
        }).start();
    }

}
