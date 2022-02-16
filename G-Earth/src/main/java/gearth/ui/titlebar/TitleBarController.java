package gearth.ui.titlebar;

import gearth.GEarth;
import gearth.ui.themes.ThemeFactory;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Optional;

public class TitleBarController {

    public Label titleLabel;
    public Pane titleBar;
    public ImageView titleIcon;
    public ImageView themeBtn;
    public ImageView minimizeBtn;

    private Stage stage;
    private TitleBarConfig config;

    private Alert alert = null;

    public static TitleBarController create(Stage stage, TitleBarConfig config) throws IOException {
        FXMLLoader loader = new FXMLLoader(TitleBarController.class.getResource("Titlebar.fxml"));
        Parent titleBar = loader.load();
        TitleBarController controller = initNewController(loader, stage, config);

        VBox newParent = new VBox(titleBar, stage.getScene().getRoot());
        newParent.setId("titlebar-main-container");
        stage.getScene().setRoot(newParent);

        return controller;
    }

    public static TitleBarController create(Alert alert) throws IOException {

        FXMLLoader loader = new FXMLLoader(TitleBarController.class.getResource("Titlebar.fxml"));
        Parent titleBar = loader.load();
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();

        TitleBarConfig config = new GEarthThemedTitleBarConfig(stage) {
            @Override
            public boolean displayMinimizeButton() {
                return false;
            }
        };
        TitleBarController controller = initNewController(loader, stage, config);

        controller.alert = alert;
        Parent parent = alert.getDialogPane().getScene().getRoot();
        VBox newParent = new VBox(titleBar, parent);
        newParent.setId("titlebar-main-container");
        stage.setScene(new Scene(newParent));
        stage.getScene().setFill(Color.TRANSPARENT);

        return controller;
    }

    private static TitleBarController initNewController(FXMLLoader loader, Stage stage, TitleBarConfig config) throws IOException {
        TitleBarController controller = loader.getController();

        controller.stage = stage;
        controller.config = config;
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.getScene().setFill(Color.TRANSPARENT);
        stage.getScene().getRoot().getStyleClass().add("root-node");

        stage.titleProperty().addListener((i) -> controller.setTitle(stage.getTitle()));
        controller.setTitle(stage.getTitle());

        stage.getIcons().addListener((InvalidationListener) observable -> controller.updateIcon());
        controller.updateIcon();

        Platform.runLater(() -> {
            controller.themeBtn.setVisible(config.displayThemePicker());
            if (!config.displayMinimizeButton()) {
                ((GridPane) controller.minimizeBtn.getParent()).getChildren().remove(controller.minimizeBtn);
            }
        });
        return controller;
    }

    public void updateIcon() {
        Platform.runLater(() -> titleIcon.setImage(stage.getIcons().size() > 0 ? stage.getIcons().get(0) :
                new Image(GEarth.class.getResourceAsStream(
                        String.format("/gearth/ui/themes/%s/logoSmall.png", ThemeFactory.getDefaultTheme().internalName())))));
    }

    public void setTitle(String title) {
        Platform.runLater(() -> titleLabel.setText(title));
    }


    public void handleCloseAction(MouseEvent event) {
        config.onCloseClicked();
    }

    public void handleMinimizeAction(MouseEvent event) {
        config.onMinimizeClicked();
    }

    private double xOffset, yOffset;
    private boolean isMoving = false;

    public void handleClickAction(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
        isMoving = true;
    }

    public void handleMovementAction(MouseEvent event) {
        if (isMoving) {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        }
    }

    public void handleClickReleaseAction(MouseEvent mouseEvent) {
        isMoving = false;
    }

    public void toggleTheme(MouseEvent event) {
        int themeIndex = ThemeFactory.allThemes().indexOf(config.getCurrentTheme());
        config.setTheme(ThemeFactory.allThemes().get((themeIndex + 1) % ThemeFactory.allThemes().size()));
    }

    public void showAlert() {
        if (alert != null) {
            alert.show();
            Platform.runLater(() -> stage.sizeToScene());
        }
    }

    public Optional<ButtonType> showAlertAndWait() {
        if (alert != null) {
            Platform.runLater(() -> stage.sizeToScene());
            return alert.showAndWait();
        }
        return Optional.empty();
    }

}
