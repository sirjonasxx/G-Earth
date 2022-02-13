package gearth.ui.themes;

public class LightTheme implements Theme {
    @Override
    public String title() {
        return "G-Earth";
    }

    @Override
    public String internalName() {
        return "G-Earth";
    }

    @Override
    public boolean overridesLogo() {
        return true;
    }

    @Override
    public boolean overridesTitle() {
        return true;
    }
}
