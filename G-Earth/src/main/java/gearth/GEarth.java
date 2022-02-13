package gearth;

import gearth.misc.AdminValidator;
import gearth.misc.Cacher;
import gearth.misc.UpdateChecker;
import gearth.ui.GEarthController;
import gearth.ui.titlebar.TitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class GEarth extends Application {

    public static Application main;
    public static String version = "1.5.1";
    public static String gitApi = "https://api.github.com/repos/sirjonasxx/G-Earth/releases/latest";
    public static String theme = "G-Earth_Dark";
    public static String[] themes = new String[] {"G-Earth", "Tanji", "G-Earth_Dark"};

    private Stage stage;
    private TitleBarController titleBar;
    private GEarthController controller;

    static {
        if (Cacher.getCacheContents().has("theme")) {
            theme = Cacher.getCacheContents().getString("theme");
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        main = this;
        stage = primaryStage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gearth/ui/G-Earth.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        controller.setStage(primaryStage);
        stage.initStyle(StageStyle.TRANSPARENT);

        primaryStage.setScene(new Scene(root));
        titleBar = TitleBarController.create(primaryStage, new TitleBarConfig() {
            @Override
            public boolean displayThemePicker() {
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
            public void onSetTheme(String theme) {
                setTheme(theme);
            }
        });
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();

        setTheme(theme);

        primaryStage.show();
        primaryStage.setOnCloseRequest( event -> closeGEarth());

        AdminValidator.validate();
        UpdateChecker.checkForUpdates();

    }

    private void closeGEarth() {
        controller.exit();
        Platform.exit();
        System.exit(0);
    }

    private void setTheme(String theme) {
        GEarth.theme = theme;

        stage.getScene().getStylesheets().clear();
        stage.getScene().getStylesheets().add(GEarth.class.getResource(String.format("/gearth/themes/%s/styling.css", theme)).toExternalForm());

        stage.getIcons().clear();
        stage.getIcons().add(new Image(GEarth.class.getResourceAsStream(String.format("/gearth/themes/%s/logoSmall.png", theme))));

        stage.setTitle(theme.split("_")[0] + " " + GEarth.version);
        titleBar.setTitle(stage.getTitle());

        controller.infoController.img_logo.setImage(new Image(GEarth.class.getResourceAsStream(String.format("/gearth/themes/%s/logo.png", theme))));
        controller.infoController.version.setText(stage.getTitle());
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
}
