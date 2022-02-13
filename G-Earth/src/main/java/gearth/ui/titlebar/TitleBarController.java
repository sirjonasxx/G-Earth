package gearth.ui.titlebar;

import gearth.GEarth;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Arrays;

public class TitleBarController {

    public Label titleLabel;
    public Pane titleBar;

    private Stage stage;
    private TitleBarConfig config;

    public static TitleBarController create(Stage stage, TitleBarConfig config) throws IOException {
        FXMLLoader loader = new FXMLLoader(TitleBarController.class.getResource("Titlebar.fxml"));
        Parent root = loader.load();
        TitleBarController controller = loader.getController();
        controller.stage = stage;
        controller.config = config;
        stage.initStyle(StageStyle.TRANSPARENT);

        Parent parent = stage.getScene().getRoot();

        VBox newParent = new VBox(root, parent);
        stage.getScene().setRoot(newParent);
        parent.getScene().setFill(Color.TRANSPARENT);

        return controller;
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }


    public void handleCloseAction(MouseEvent event) {
        config.onCloseClicked();
    }

    public void handleMinimizeAction(MouseEvent event) {
        stage.setIconified(true);
    }

    private double xOffset, yOffset;

    public void handleClickAction(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    public void handleMovementAction(MouseEvent event) {
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    public void toggleTheme(MouseEvent event) {
        int themeIndex = Arrays.asList(GEarth.themes).indexOf(GEarth.theme);
        config.onSetTheme(GEarth.themes[(themeIndex + 1) % GEarth.themes.length]);
    }

}
