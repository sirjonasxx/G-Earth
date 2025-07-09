package gearth.ui.themes;

import java.util.Arrays;
import java.util.List;

public class ThemeFactory {

    private static List<Theme> themes = Arrays.asList(
            new LightTheme(),
            new TanjiTheme(),
            new DarkTheme()
    );

    public static Theme getDefaultTheme() {
        return themes.get(0);
    }

    public static List<Theme> allThemes() {
        return themes;
    }

    // returns default theme if not found
    public static Theme themeForTitle(String title) {
        return allThemes().stream().filter(theme -> theme.title().equals(title)).findFirst().orElse(getDefaultTheme());
    }

}
