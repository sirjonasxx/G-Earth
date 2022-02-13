package gearth.misc;

import gearth.GEarth;
import gearth.ui.titlebar.DefaultTitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintStream;
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
                    stage.getIcons().add(new Image(GEarth.class.getResourceAsStream("/gearth/ui/themes/G-Earth/logoSmall.png")));
                    stage.getScene().getStylesheets().add(GEarth.class.getResource(String.format("/gearth/ui/themes/%s/styling.css", GEarth.theme.internalName())).toExternalForm());
//                    try {
//                        TitleBarController.create(stage, new DefaultTitleBarConfig(stage));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    alert.getDialogPane().setMaxHeight(-1);
//                    alert.getDialogPane().setMinHeight(200);
//                    alert.getDialogPane()
//
//
//                    boolean[] once = new boolean[]{false};
//                    stage.heightProperty().addListener(observable -> {
//                        if (!once[0]) {
//                            once[0] = true;
//                            stage.setMinHeight(alert.getDialogPane().getHeight() + 25);
//                            stage.setHeight(alert.getDialogPane().getHeight() + 25);
//                            stage.setMaxHeight(alert.getDialogPane().getHeight() + 25);
//                        }
//
//                    });

//                    stage.setHeight(stage.getHeight() + 25);
//                    stage.setResizable(false);
//                    stage.sizeToScene();x
                    stage.show();
                });

            }
        }).start();
    }

}
