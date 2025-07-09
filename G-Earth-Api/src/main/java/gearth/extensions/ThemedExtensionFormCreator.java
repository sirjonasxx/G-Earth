package gearth.extensions;

import gearth.ui.themes.Theme;
import gearth.ui.themes.ThemeFactory;
import gearth.ui.titlebar.DefaultTitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public abstract class ThemedExtensionFormCreator extends ExtensionFormCreator {

    @Override
    protected ExtensionForm createForm(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getFormResource());
        Parent root = loader.load();

        primaryStage.setTitle(getTitle());
        primaryStage.setScene(new Scene(root));
        initialize(primaryStage);
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();

        Theme defaultTheme = ThemeFactory.getDefaultTheme();
        DefaultTitleBarConfig config = new DefaultTitleBarConfig(primaryStage, defaultTheme) {
            @Override
            public boolean displayThemePicker() {
                return false;
            }
        };
        TitleBarController.create(primaryStage, config);
        Platform.runLater(() -> {
            primaryStage.getScene().getRoot().getStyleClass().add(defaultTheme.title().replace(" ", "-").toLowerCase());
            primaryStage.getScene().getRoot().getStyleClass().add(defaultTheme.isDark() ? "g-dark" : "g-light");
        });

        ExtensionForm extensionForm = loader.getController();
        extensionForm.fieldsInitialized.addListener(() -> extensionForm.extension.observableHostInfo.addListener(hostInfo -> {
            if (hostInfo.getAttributes().containsKey("theme")) {
                String themeTitle = hostInfo.getAttributes().get("theme");
                Theme theme = ThemeFactory.themeForTitle(themeTitle);
                if (config.getCurrentTheme() != theme) {
                    String styleClassOld = config.getCurrentTheme().title().replace(" ", "-").toLowerCase();
                    String lightClassOld = config.getCurrentTheme().isDark() ? "g-dark" : "g-light";
                    String styleClassNew = theme.title().replace(" ", "-").toLowerCase();
                    String lightClassNew = theme.isDark() ? "g-dark" : "g-light";
                    config.setTheme(theme);
                    Parent currentRoot = primaryStage.getScene().getRoot();
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
        }));


        return extensionForm;
    }

    protected abstract String getTitle();
    protected abstract URL getFormResource();

    // can be overridden for more settings
    protected void initialize(Stage primaryStage) {

    }
}
