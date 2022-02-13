package gearth.ui.titlebar;

public interface TitleBarConfig {

    boolean displayThemePicker();

    void onCloseClicked();
    void onMinimizeClicked();
    void onSetTheme(String theme);
}
