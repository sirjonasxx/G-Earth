package gearth.ui.titlebar;

import gearth.GEarth;
import javafx.stage.Stage;

public class GEarthThemedTitleBarConfig extends DefaultTitleBarConfig {

    public GEarthThemedTitleBarConfig(Stage stage) {
        super(stage, GEarth.getTheme());
        GEarth.getThemeObservable().addListener(this::setTheme);
    }

    @Override
    public boolean displayThemePicker() {
        return false;
    }

}
