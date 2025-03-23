package gearth;

import gearth.misc.AdminValidator;
import gearth.misc.Cacher;
import gearth.misc.UpdateChecker;
import gearth.misc.listenerpattern.ObservableObject;
import gearth.ui.GEarthController;
import gearth.ui.GEarthTrayIcon;
import gearth.ui.themes.Theme;
import gearth.ui.themes.ThemeFactory;
import gearth.ui.titlebar.TitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Properties;

public class GEarth extends Application {

    public static final String OFFICIAL_REPOSITORY = "sirjonasxx/G-Earth";

    private static final Logger logger = LoggerFactory.getLogger(GEarth.class);

    public static GEarth main;
    public static String version;
    public static String storeVersion;
    public static String repository;
    public static ObservableObject<Theme> observableTheme;

    private Stage stage;
    private GEarthController controller;

    static {
        observableTheme = new ObservableObject<>(
                Cacher.getCacheContents().has("theme") ?
                        ThemeFactory.themeForTitle(Cacher.getCacheContents().getString("theme")) :
                        ThemeFactory.getDefaultTheme()
        );

        // Bridge JUL to SLF4J.
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        // Load build.properties
        try {
            final Properties buildProperties = new Properties();
            buildProperties.load(GEarth.class.getResourceAsStream("/build.properties"));

            version = buildProperties.getProperty("build.version");
            storeVersion = buildProperties.getProperty("build.storeVersion");
            repository = buildProperties.getProperty("build.github");

            logger.info("Starting G-Earth {} from repository {}", version, repository);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
        stage.initStyle(StageStyle.TRANSPARENT);

        primaryStage.setScene(new Scene(root));
        TitleBarController.create(primaryStage, new TitleBarConfig() {
            @Override
            public boolean displayThemePicker() {
                return true;
            }

            @Override
            public boolean displayMinimizeButton() {
                return true;
            }

//            @Override
//            public boolean allowResizing() {
//                return false;
//            }

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
                setGearthTheme(theme);
            }

            @Override
            public Theme getCurrentTheme() {
                return observableTheme.getObject();
            }
        });
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();

        setGearthTheme(observableTheme.getObject());

        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> closeGEarth());

        //AdminValidator.validate();
        //UpdateChecker.checkForUpdates();

    }

    private void closeGEarth() {
        controller.exit();
        Platform.exit();
        System.exit(0);
    }

    private void setGearthTheme(Theme theme) {
        Cacher.put("theme", theme.title());
        observableTheme.setObject(theme);
        Theme defaultTheme = ThemeFactory.getDefaultTheme();

//        Platform.runLater(() -> {
            stage.getScene().getStylesheets().clear();
            stage.getScene().getStylesheets().add(GEarth.class.getResource(String.format("/gearth/ui/themes/%s/styling.css", theme.internalName())).toExternalForm());

            stage.getIcons().clear();
            final Image image = new Image(GEarth.class.getResourceAsStream(String.format("/gearth/ui/themes/%s/logoSmall.png", theme.overridesLogo() ? theme.internalName() : defaultTheme.internalName())));
            stage.getIcons().add(image);
            final String isFork = GEarth.repository.equals(OFFICIAL_REPOSITORY) ? "" : " (" + GEarth.repository + ")";
            stage.setTitle((theme.overridesTitle() ? theme.title() : defaultTheme.title()) + " " + GEarth.version + isFork);

            controller.infoController.img_logo.setImage(new Image(GEarth.class.getResourceAsStream(
                    String.format(
                            "/gearth/ui/themes/%s/logo.png",
                            theme.overridesLogo() ? theme.internalName() : defaultTheme.internalName()
                    )
            )));
            controller.infoController.version.setText(stage.getTitle());
//        });
        GEarthTrayIcon.updateOrCreate(image);
    }

    public GEarthController getController() {
        return controller;
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

    public static ObservableObject<Theme> getThemeObservable() {
        return observableTheme;
    }

    public static Theme getTheme() {
        return observableTheme.getObject();
    }

    public static void setAlertOwner(Alert alert) {
        alert.initOwner(main.stage);
    }
}
