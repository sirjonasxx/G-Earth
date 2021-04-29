package gearth;

import gearth.misc.AdminValidator;
import gearth.ui.GEarthController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

// run as root issue Invalid MIT-MAGIC-COOKIE-1 key fix: https://stackoverflow.com/questions/48139447/invalid-mit-magic-cookie-1-key

public class Main extends Application {

    public static Application main;
    public static String version = "1.4.1";
    private static String gitApi = "https://api.github.com/repos/sirjonasxx/G-Earth/releases/latest";

    @Override
    public void start(Stage primaryStage) throws Exception{
        main = this;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("ui/G-Earth.fxml"));
        Parent root = loader.load();
        GEarthController companion = loader.getController();
        companion.setStage(primaryStage);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("G-EarthLogoSmaller.png")));

        primaryStage.setResizable(false);

        primaryStage.setTitle("G-Earth " + version);
        primaryStage.setScene(new Scene(root, 650, 295));
        primaryStage.show();
        primaryStage.getScene().getStylesheets().add(getClass().getResource("ui/bootstrap3.css").toExternalForm());

        primaryStage.setOnCloseRequest( event -> {
            companion.exit();
            Platform.exit();

            // Platform.exit doesn't seem to be enough on Windows?
            System.exit(0);
        });

        new Thread(() -> {
            if (!AdminValidator.isAdmin()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "G-Earth needs admin privileges in order to work on Flash, please restart G-Earth with admin permissions unless you're using Unity", ButtonType.OK);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.setResizable(false);
                    alert.show();
                });

            }
        }).start();

        new Thread(() -> {
            try {
                String s = Jsoup.connect(gitApi).ignoreContentType(true).get().body().toString();
                s = s.substring(6, s.length() - 7);
                JSONObject object = new JSONObject(s);
                String gitv = (String)object.get("tag_name");
                if (!gitv.equals(version)) {
                    Platform.runLater(() -> {
                        String body = (String)object.get("body");
                        boolean isForcedUpdate = body.contains("(!)");

                        Alert alert = new Alert(isForcedUpdate ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION, "G-Earth is outdated!", ButtonType.OK);

                        FlowPane fp = new FlowPane();
                        Label lbl = new Label("A new version of G-Earth has been found ("+gitv+")" + System.lineSeparator()+ System.lineSeparator() + "Update to the latest version:");
                        Hyperlink link = new Hyperlink("https://github.com/sirjonasxx/G-Earth/releases");
                        fp.getChildren().addAll( lbl, link);
                        link.setOnAction(event -> {
                            Main.main.getHostServices().showDocument(link.getText());
                            event.consume();
                        });



                        WebView webView = new WebView();
                        webView.getEngine().loadContent("<html>A new version of G-Earth has been found ("+gitv+")<br><br>Update to the latest version:<br><a href=\"https://github.com/sirjonasxx/G-Earth/releases\">https://github.com/sirjonasxx/G-Earth/releases</a></html>");
                        webView.setPrefSize(500, 200);

                        alert.setResizable(false);
                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        alert.getDialogPane().setContent(fp);
                        if (isForcedUpdate) {
                            alert.setOnCloseRequest(event -> System.exit(0));
                        }
                        alert.show();

                    });
                }

            } catch (IOException e) {
//                e.printStackTrace();
            }
        }).start();

    }

    public static String[] args;

    public static void main(String[] args) {
        Main.args = args;
        launch(args);
    }

    public static boolean hasFlag(String flag) {
        for(String s : args) {
            if (s.equals(flag)) {
                return true;
            }
        }
        return false;
    }

    public static String getArgument(String... arg) {
        for (int i = 0; i < args.length - 1; i++) {
            for (String str : arg) {
                if (args[i].toLowerCase().equals(str.toLowerCase())) {
                    return args[i+1];
                }
            }
        }
        return null;
    }
}

// Hi
// I'm
// Lande
// I want
// The role
// Developer
// Pls
// You say :
// Change 10 lines
// I dit it.
// https://i.imgur.com/QEHV2NZ.png
