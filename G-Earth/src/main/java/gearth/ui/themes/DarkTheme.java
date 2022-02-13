package gearth.ui.themes;

public class DarkTheme implements Theme {
    @Override
    public String title() {
        return "G-Earth Dark";
    }

    @Override
    public String internalName() {
        return "G-Earth_Dark";
    }

    @Override
    public boolean overridesLogo() {
        return false;
    }

    @Override
    public boolean overridesTitle() {
        return false;
    }
}
