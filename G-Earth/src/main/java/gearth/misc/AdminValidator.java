package gearth.misc;

import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

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
                    Alert alert = new Alert(Alert.AlertType.WARNING, "", ButtonType.OK);
                    alert.getDialogPane().setContent(new Label("G-Earth needs admin privileges in order to work on Flash,\nplease restart G-Earth with admin permissions unless\nyou're using Unity"));
                    try {
                        TitleBarController.create(alert).showAlert();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            }
        }).start();
    }

}
