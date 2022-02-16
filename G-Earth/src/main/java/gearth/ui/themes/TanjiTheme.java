package gearth.ui.themes;

public class TanjiTheme implements Theme {
    @Override
    public String title() {
        return "Tanji";
    }

    @Override
    public String internalName() {
        return "Tanji";
    }

    @Override
    public boolean isDark() {
        return false;
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
