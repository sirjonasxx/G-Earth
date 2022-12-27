package gearth.ui.translations;

import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public enum Language {
    ENGLISH     ("en"),
    DUTCH       ("nl"),
    FRENCH      ("fr"),
    GERMAN      ("de"),
    SPANISH     ("es"),
    PORTUGUESE  ("pt"),
    ITALIAN     ("it"),
    FINNISH     ("fi"),
    TURKISH     ("tr");

    public final ResourceBundle messages;
    private final String locale;

    Language(String locale) {
        this.locale = locale;

        ResourceBundle resBundle;
        try {
            InputStream stream = Language.class.getResourceAsStream(String.format("/gearth/ui/translations/messages_%s.properties", locale));
            InputStreamReader isr = new InputStreamReader(stream, StandardCharsets.UTF_8);
            resBundle = new PropertyResourceBundle(isr);
        } catch (Exception e) {
            System.out.printf("/gearth/ui/translations/messages_%s.properties%n", locale);
            System.out.println("Couldn't load language file: " + locale);
            resBundle = null;
        }

        this.messages = resBundle;
    }

    public MenuItem asMenuItem() {
        MenuItem menuItem = new MenuItem(null, getIcon());
        menuItem.setOnAction(this::onSelect);
        return menuItem;
    }

    private void onSelect(ActionEvent actionEvent) {
        ContextMenu  ctxMenu = ((MenuItem) actionEvent.getSource()).getParentPopup();
        ((MenuButton) ctxMenu.getOwnerNode()).setGraphic(getIcon());
        LanguageBundle.setLanguage(this);
    }

    public ImageView getIcon() {
        ImageView icon = new ImageView();
        icon.getStyleClass().addAll("language-icon", locale);
        icon.setFitWidth(18);
        icon.setFitHeight(18);
        return icon;
    }

    public static MenuItem[] getMenuItems() {
        return Arrays.stream(values())
                .map(Language::asMenuItem)
                .toArray(MenuItem[]::new);
    }

    public static Language getSystemLanguage() {
        String locale = System.getProperty("user.language");
        for (Language l : values())
            if (l.locale.equals(locale))
                return l;

        return ENGLISH;
    }
}
