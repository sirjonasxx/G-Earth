package gearth.ui.titlebar;

import gearth.GEarth;
import gearth.ui.themes.Theme;
import gearth.ui.themes.ThemeFactory;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

// a typical config to be used in all windows that is not the g-earth main window
public class DefaultTitleBarConfig implements TitleBarConfig {

    protected Stage stage;
    private String currentStylesheet = null;
    private Theme currentTheme;


    public DefaultTitleBarConfig(Stage stage) {
        this(stage, ThemeFactory.getDefaultTheme());
    }

    public DefaultTitleBarConfig(Stage stage, Theme theme) {
        this.stage = stage;
        currentTheme = theme;
        setTheme(theme);
    }

    @Override
    public boolean displayThemePicker() {
        return true;
    }

    @Override
    public boolean displayMinimizeButton() {
        return true;
    }

//    @Override
//    public boolean allowResizing() {
//        return false;
//    }

    @Override
    public void onCloseClicked() {
        stage.close();
    }

    @Override
    public void onMinimizeClicked() {
        stage.setIconified(true);
    }

    @Override
    public void setTheme(Theme theme) {
        currentTheme = theme;
        Platform.runLater(() -> {
            Theme defaultTheme = ThemeFactory.getDefaultTheme();
            if (currentStylesheet != null) {
                stage.getScene().getStylesheets().remove(currentStylesheet);
            }
            currentStylesheet = GEarth.class.getResource(String.format("/gearth/ui/themes/%s/styling.css", theme.internalName())).toExternalForm();
            stage.getScene().getStylesheets().add(currentStylesheet);

            stage.getIcons().clear();
            stage.getIcons().add(new Image(GEarth.class.getResourceAsStream(String.format("/gearth/ui/themes/%s/logoSmall.png", theme.overridesLogo() ? theme.internalName() : defaultTheme.internalName()))));

        });
    }

    @Override
    public Theme getCurrentTheme() {
        return currentTheme;
    }
}
