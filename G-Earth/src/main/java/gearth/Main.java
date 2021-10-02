package gearth;

import gearth.misc.AdminValidator;
import gearth.misc.Cacher;
import gearth.misc.UpdateChecker;
import gearth.ui.GEarthController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    public static Application main;
    public static String version = "1.5";
    public static String gitApi = "https://api.github.com/repos/sirjonasxx/G-Earth/releases/latest";
    public static String theme = "G-Earth";

    static {
        if (Cacher.getCacheContents().has("theme")) {
            theme = Cacher.getCacheContents().getString("theme");
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        main = this;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gearth/ui/G-Earth.fxml"));
        Parent root = loader.load();
        GEarthController companion = loader.getController();
        companion.setStage(primaryStage);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream(String.format("/gearth/themes/%s/logoSmall.png", theme))));

        primaryStage.setTitle(theme + " " + version);
        // https://stackoverflow.com/questions/20732100/javafx-why-does-stage-setresizablefalse-cause-additional-margins
//        primaryStage.setScene(new Scene(root, 650, 295));
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.sizeToScene();

        primaryStage.show();
        primaryStage.getScene().getStylesheets().add(getClass().getResource(String.format("/gearth/themes/%s/styling.css", theme)).toExternalForm());

        primaryStage.setOnCloseRequest( event -> {
            companion.exit();
            Platform.exit();

            // Platform.exit doesn't seem to be enough on Windows?
            System.exit(0);
        });

        AdminValidator.validate();
        UpdateChecker.checkForUpdates();

    }

    public static String[] args;

    public static void main(String[] args) {
        Main.args = args;
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
