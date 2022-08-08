package gearth.ui.translations;

import javafx.event.ActionEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;

import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

public enum Language {
    DEFAULT     ("en"),
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
        messages = ResourceBundle.getBundle("gearth.ui.translations.messages", new Locale(locale));
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
}
