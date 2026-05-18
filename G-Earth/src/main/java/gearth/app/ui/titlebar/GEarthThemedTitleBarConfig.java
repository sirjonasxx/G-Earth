package gearth.app.ui.titlebar;

import gearth.app.GEarth;
import gearth.ui.titlebar.DefaultTitleBarConfig;
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
