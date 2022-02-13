package gearth.ui.titlebar;

import gearth.GEarth;
import gearth.ui.themes.Theme;
import gearth.ui.themes.ThemeFactory;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class TitleBarController {

    public Label titleLabel;
    public Pane titleBar;
    public ImageView titleIcon;

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
        newParent.setId("titlebar-main-container");
        stage.getScene().setRoot(newParent);
        parent.getScene().setFill(Color.TRANSPARENT);

        stage.titleProperty().addListener((i) -> controller.setTitle(stage.getTitle()));
        controller.setTitle(stage.getTitle());

        stage.getIcons().addListener((InvalidationListener) observable -> controller.updateIcon());
        controller.updateIcon();

        return controller;
    }

    public void updateIcon() {
        titleIcon.setImage(stage.getIcons().size() > 0 ? stage.getIcons().get(0) :
                new Image(GEarth.class.getResourceAsStream(
                        String.format("/gearth/ui/themes/%s/logoSmall.png", ThemeFactory.getDefaultTheme().internalName()))));
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }


    public void handleCloseAction(MouseEvent event) {
        config.onCloseClicked();
    }

    public void handleMinimizeAction(MouseEvent event) {
        config.onMinimizeClicked();
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
        int themeIndex = ThemeFactory.allThemes().indexOf(config.getCurrentTheme());
        config.setTheme(ThemeFactory.allThemes().get((themeIndex + 1) % ThemeFactory.allThemes().size()));
    }

}
