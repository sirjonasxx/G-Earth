package gearth;

import gearth.misc.AdminValidator;
import gearth.misc.UpdateChecker;
import gearth.ui.GEarthController;
import gearth.ui.GEarthProperties;
import gearth.ui.themes.Theme;
import gearth.ui.titlebar.TitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class GEarth extends Application {

    public static GEarth main;
    public static String version = "1.5.3";
    public static String gitApi = "https://api.github.com/repos/sirjonasxx/G-Earth/releases/latest";

    private Stage stage;
    private GEarthController controller;


    @Override
    public void start(Stage primaryStage) throws Exception{
        main = this;
        stage = primaryStage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gearth/ui/G-Earth.fxml"));
        Parent root;
        try {
            root = loader.load();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        controller = loader.getController();
        controller.setStage(primaryStage);

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(new Scene(root));
        primaryStage.setAlwaysOnTop(GEarthProperties.isAlwaysOnTop());
        GEarthProperties.alwaysOnTopProperty
                .addListener((observable, oldValue, newValue) -> primaryStage.setAlwaysOnTop(newValue));

        initTitleBar(primaryStage);
        initTheme();

        primaryStage.setResizable(false);
        primaryStage.sizeToScene();

        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> closeGEarth());

        AdminValidator.validate();
        UpdateChecker.checkForUpdates();

    }

    private void initTitleBar(Stage primaryStage) throws IOException {
        TitleBarController.create(primaryStage, new TitleBarConfig() {
            @Override
            public boolean displayThemePicker() {
                return true;
            }

            @Override
            public boolean displayMinimizeButton() {
                return true;
            }

            @Override
            public void onCloseClicked() {
                closeGEarth();
            }

            @Override
            public void onMinimizeClicked() {
                stage.setIconified(true);
            }

            @Override
            public void setTheme(Theme theme) {
                GEarthProperties.themeProperty.set(theme);
            }

            @Override
            public Theme getCurrentTheme() {
                return GEarthProperties.getTheme();
            }
        });
    }

    private void initTheme() {
        stage.titleProperty().bind(GEarthProperties.themeTitleBinding);
        Bindings.bindContent(stage.getScene().getStylesheets(), GEarthProperties.styleSheets);
        Bindings.bindContent(stage.getIcons(), GEarthProperties.icons);
    }

    private void closeGEarth() {
        controller.exit();
        Platform.exit();
        System.exit(0);
    }

    public static String[] args;

    public static void main(String[] args) {
        GEarth.args = args;
        launch(args);
    }

    public static boolean hasFlag(String flag) {
        for(String s : args) {
            if (s.equals(flag)) {
                return true;
            }
        }
        return false;
    }

    public static String getArgument(String... arg) {
        for (int i = 0; i < args.length - 1; i++) {
            for (String str : arg) {
                if (args[i].toLowerCase().equals(str.toLowerCase())) {
                    return args[i+1];
                }
            }
        }
        return null;
    }

    public static void setAlertOwner(Alert alert) {
        alert.initOwner(main.stage);
    }
}
