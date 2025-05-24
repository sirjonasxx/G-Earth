package gearth.app.ui.titlebar;

import gearth.app.GEarth;
import gearth.ui.titlebar.TitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class TitleBarAlert {

    public static TitleBarController create(Alert alert) throws IOException {
        GEarth.setAlertOwner(alert);

        FXMLLoader loader = new FXMLLoader(TitleBarController.class.getResource("Titlebar.fxml"));
        Parent titleBar = loader.load();
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();

        TitleBarConfig config = new GEarthThemedTitleBarConfig(stage) {
            @Override
            public boolean displayMinimizeButton() {
                return false;
            }
        };
        TitleBarController controller = TitleBarController.initNewController(loader, stage, config);

        controller.alert = alert;
        Parent parent = alert.getDialogPane().getScene().getRoot();
        VBox newParent = new VBox(titleBar, parent);
        newParent.setId("titlebar-main-container");
        stage.setScene(new Scene(newParent));
        stage.getScene().setFill(Color.TRANSPARENT);

        return controller;
    }

}
