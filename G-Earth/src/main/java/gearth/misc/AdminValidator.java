package gearth.misc;

import gearth.Main;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.PrintStream;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Created by Jonas on 5/11/2018.
 */
public class AdminValidator {

    //https://stackoverflow.com/questions/4350356/detect-if-java-application-was-run-as-a-windows-admin

    private static Boolean isAdmin = null;

    public static boolean isAdmin() {
        if (isAdmin == null) {
            Preferences prefs = Preferences.systemRoot();
            PrintStream systemErr = System.err;
            synchronized(systemErr){    // better synchroize to avoid problems with other threads that access System.err
                System.setErr(null);
                try{
                    prefs.put("foo", "bar"); // SecurityException on Windows
                    prefs.remove("foo");
                    prefs.flush(); // BackingStoreException on Linux
                    isAdmin = true;
                }catch(Exception e){
                    isAdmin = false;
                }finally{
                    System.setErr(systemErr);
                }
            }
        }

        return isAdmin;
    }

    public static void validate() {
        new Thread(() -> {
            if (!AdminValidator.isAdmin()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "G-Earth needs admin privileges in order to work on Flash, please restart G-Earth with admin permissions unless you're using Unity", ButtonType.OK);
                    Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                    stage.getIcons().add(new Image(Main.class.getResourceAsStream(String.format("/gearth/themes/%s/logoSmall.png", Main.theme))));
                    stage.getScene().getStylesheets().add(Main.class.getResource(String.format("/gearth/themes/%s/styling.css", Main.theme)).toExternalForm());
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.setResizable(false);
                    alert.show();
                });

            }
        }).start();
    }

}
