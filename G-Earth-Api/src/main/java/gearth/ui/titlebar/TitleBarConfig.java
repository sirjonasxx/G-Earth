package gearth.ui.titlebar;

import gearth.ui.themes.Theme;

public interface TitleBarConfig {

    boolean displayThemePicker();
    boolean displayMinimizeButton();
//    boolean allowResizing();

    void onCloseClicked();
    void onMinimizeClicked();
    void setTheme(Theme theme);
    Theme getCurrentTheme();
}
