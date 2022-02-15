package gearth.ui.titlebar;

import gearth.GEarth;
import gearth.ui.themes.Theme;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class GEarthThemedTitleBarConfig extends DefaultTitleBarConfig {

    public GEarthThemedTitleBarConfig(Stage stage) {
        super(stage, GEarth.theme);
        GEarth.themeObservable.addListener(this::setTheme);
    }

    @Override
    public boolean displayThemePicker() {
        return false;
    }

}
