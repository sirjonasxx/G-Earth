package gearth.extensions;

import gearth.ui.themes.Theme;
import gearth.ui.themes.ThemeFactory;
import gearth.ui.titlebar.DefaultTitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public abstract class ThemedExtensionFormCreator extends ExtensionFormCreator {

    @Override
    protected ExtensionForm createForm(Stage primaryStage) throws Exception {
        final FXMLLoader loader = new FXMLLoader(getFormResource());
        final Parent root = loader.load();

        primaryStage.setTitle(getTitle());
        primaryStage.setScene(new Scene(root));
        initialize(primaryStage);
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();

        final Theme defaultTheme = ThemeFactory.getDefaultTheme();
        final DefaultTitleBarConfig config = new DefaultTitleBarConfig(primaryStage, defaultTheme) {
            @Override
            public boolean displayThemePicker() {
                return false;
            }
        };
        TitleBarController.create(primaryStage, config);
        Platform.runLater(() -> primaryStage.getScene().getRoot().getStyleClass().addAll(
                defaultTheme.title().replace(" ", "-").toLowerCase(),
                defaultTheme.isDark() ? "g-dark" : "g-light"
        ));

        final ExtensionForm extensionForm = loader.getController();
        extensionForm
                .fieldsInitialisedProperty()
                .addListener(observable -> listenForThemeChange(primaryStage, config, extensionForm));
        return extensionForm;
    }

    private static void listenForThemeChange(Stage primaryStage, DefaultTitleBarConfig config, ExtensionForm extensionForm) {
        extensionForm.extension.hostInfoProperty.addListener((observable, oldValue, newValue) -> {
            final String themeTitle = newValue.getAttributes().get("theme");
            if (themeTitle != null) {
                final Theme theme = ThemeFactory.themeForTitle(themeTitle);
                if (config.getCurrentTheme() != theme) {
                    final String styleClassOld = config.getCurrentTheme().title().replace(" ", "-").toLowerCase();
                    final String lightClassOld = config.getCurrentTheme().isDark() ? "g-dark" : "g-light";
                    final String styleClassNew = theme.title().replace(" ", "-").toLowerCase();
                    final String lightClassNew = theme.isDark() ? "g-dark" : "g-light";
                    config.setTheme(theme);
                    final Parent currentRoot = primaryStage.getScene().getRoot();
                    Platform.runLater(() -> {
                        currentRoot.getStyleClass().remove(styleClassOld);
                        currentRoot.getStyleClass().add(styleClassNew);
                        if (!lightClassOld.equals(lightClassNew)) {
                            currentRoot.getStyleClass().remove(lightClassOld);
                            currentRoot.getStyleClass().add(lightClassNew);
                        }
                    });
                }
            }
        });
    }

    protected abstract String getTitle();

    protected abstract URL getFormResource();

    // can be overridden for more settings
    protected void initialize(Stage primaryStage) {

    }
}
