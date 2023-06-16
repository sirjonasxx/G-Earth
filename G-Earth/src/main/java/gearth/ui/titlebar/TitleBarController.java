package gearth.ui.titlebar;

import gearth.GEarth;
import gearth.misc.BindingsUtil;
import gearth.ui.GEarthProperties;
import gearth.ui.themes.ThemeFactory;
import gearth.ui.translations.Language;
import gearth.ui.translations.LanguageBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Optional;

public class TitleBarController {

    public Label titleLabel;
    public ImageView titleIcon;
    public ImageView themeButton;
    public ImageView minimizeButton;
    public MenuButton languagePickerMenu;

    private Stage stage;
    private TitleBarConfig config;

    private Alert alert = null;
    private double xOffset, yOffset;
    private boolean isMoving = false;

    public static TitleBarController create(Stage stage, TitleBarConfig config) throws IOException {
        final FXMLLoader loader = new FXMLLoader(TitleBarController.class.getResource("Titlebar.fxml"));
        final Parent titleBar = loader.load();
        final TitleBarController controller = initNewController(loader, stage, config);

        final VBox newParent = new VBox(titleBar, stage.getScene().getRoot());
        newParent.setId("titlebar-main-container");

        stage.getScene().setRoot(newParent);
        return controller;
    }

    public static TitleBarController create(Alert alert) throws IOException {

        GEarth.setAlertOwner(alert);

        final FXMLLoader loader = new FXMLLoader(TitleBarController.class.getResource("Titlebar.fxml"));
        final Parent titleBar = loader.load();
        final Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();

        final TitleBarConfig config = new GEarthThemedTitleBarConfig(stage) {
            @Override
            public boolean displayMinimizeButton() {
                return false;
            }
        };

        final TitleBarController controller = initNewController(loader, stage, config);
        controller.alert = alert;

        final Parent parent = alert.getDialogPane().getScene().getRoot();
        final VBox newParent = new VBox(titleBar, parent);
        newParent.setId("titlebar-main-container");

        stage.setScene(new Scene(newParent));
        stage.getScene().setFill(Color.TRANSPARENT);

        return controller;
    }

    private static TitleBarController initNewController(FXMLLoader loader, Stage stage, TitleBarConfig config) {

        stage.initStyle(StageStyle.TRANSPARENT);

        final TitleBarController controller = loader.getController();
        controller.stage = stage;
        controller.config = config;
        controller.languagePickerMenu.getItems().setAll(Language.getMenuItems());
        controller.languagePickerMenu.setGraphic(LanguageBundle.getLanguage().getIcon());

        BindingsUtil.setAndBind(
                controller.titleLabel.textProperty(),
                GEarthProperties.themeTitleBinding,
                true);

        BindingsUtil.setAndBind(
                controller.titleIcon.imageProperty(),
                GEarthProperties.logoSmallImageBinding,
                true);

        Platform.runLater(() -> {
            stage.getScene().setFill(Color.TRANSPARENT);
            stage.getScene().getRoot().getStyleClass().add("root-node");

            if (!config.displayMinimizeButton()) {
                ((GridPane) controller.minimizeButton.getParent()).getChildren().remove(controller.minimizeButton);
            }

            if (!config.displayThemePicker()) {
                ((GridPane) controller.themeButton.getParent()).getChildren().remove(controller.themeButton);
                ((GridPane) controller.languagePickerMenu.getParent()).getChildren().remove(controller.languagePickerMenu);
            }
        });

        return controller;
    }

    @FXML
    public void onClickCloseButton() {
        config.onCloseClicked();
    }

    @FXML
    public void onClickMinimizeButton() {
        config.onMinimizeClicked();
    }

    @FXML
    public void onClick(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
        isMoving = true;
    }

    @FXML
    public void onMovement(MouseEvent event) {
        if (isMoving) {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        }
    }

    @FXML
    public void onPressReleased() {
        isMoving = false;
    }

    @FXML
    public void toggleTheme() {
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
