package gearth.ui.themes;

public interface Theme {

    String title();
    String internalName();

    boolean isDark();
    boolean overridesLogo();
    boolean overridesTitle();

}
