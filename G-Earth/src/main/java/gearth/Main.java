package gearth;

import gearth.misc.AdminValidator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import gearth.ui.GEarthController;

// run as root issue Invalid MIT-MAGIC-COOKIE-1 key fix: https://stackoverflow.com/questions/48139447/invalid-mit-magic-cookie-1-key

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gearth/ui/G-Earth.fxml"));
        Parent root = loader.load();

        GEarthController companion = loader.getController();
        companion.setStage(primaryStage);

        primaryStage.setResizable(false);
        //primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("G-Earth");
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
