package gearth.ui.titlebar;

import gearth.ui.GEarthProperties;
import javafx.stage.Stage;

public class GEarthThemedTitleBarConfig extends DefaultTitleBarConfig {

    public GEarthThemedTitleBarConfig(Stage stage) {
        super(stage, GEarthProperties.getTheme());
        GEarthProperties.themeProperty.addListener((observable, oldValue, newValue) -> setTheme(newValue));
    }

    @Override
    public boolean displayThemePicker() {
        return false;
    }

}
