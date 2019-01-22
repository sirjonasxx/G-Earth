package gearth;

import gearth.misc.AdminValidator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import gearth.ui.GEarthController;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.omg.CORBA.Environment;

import java.io.IOException;

// run as root issue Invalid MIT-MAGIC-COOKIE-1 key fix: https://stackoverflow.com/questions/48139447/invalid-mit-magic-cookie-1-key

public class Main extends Application {

    public static Application main;
    public static String version = "0.2.2";
    private static String gitApi = "https://api.github.com/repos/sirjonasxx/G-Earth/releases/latest";

    @Override
    public void start(Stage primaryStage) throws Exception{
        main = this;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gearth/ui/G-Earth.fxml"));
        Parent root = loader.load();

        GEarthController companion = loader.getController();
        companion.setStage(primaryStage);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/gearth/G-EarthLogoSmaller.png")));

        primaryStage.setResizable(false);
        //primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("G-Earth " + version);
        primaryStage.setScene(new Scene(root, 620, 295));
        primaryStage.show();
        primaryStage.getScene().getStylesheets().add(getClass().getResource("/gearth/ui/bootstrap3.css").toExternalForm());

        primaryStage.setOnCloseRequest( event -> {
            companion.abort();
            Platform.exit();

            // Platform.exit doesn't seem to be enough on Windows?
            System.exit(0);
        });

        new Thread(() -> {
            if (!AdminValidator.isAdmin()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "G-Earth needs admin privileges in order to work properly, please restart G-Earth with admin permissions unless you know what you're doing", ButtonType.OK);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
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
                        boolean isForcedUpdate = body.contains("<F>");

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
}
