package gearth.app.ui.subforms.extensions.logger;

import gearth.app.ui.titlebar.GEarthThemedTitleBarConfig;
import gearth.ui.titlebar.TitleBarController;
import gearth.app.ui.translations.TranslatableString;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExtensionLogger {
    private Stage stage;
    private ExtensionLoggerController controller = null;

    private final List<String> appendOnLoad = new ArrayList<>();

    volatile boolean isVisible = false;

    public ExtensionLogger() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gearth/ui/subforms/extensions/logger/ExtensionLogger.fxml"));

        try {
            Parent root = loader.load();
            synchronized (appendOnLoad) {
                controller = loader.getController();
                for (String s : appendOnLoad) {
                    controller.log(s);
                }
                appendOnLoad.clear();
            }


            stage = new Stage();
            stage.titleProperty().bind(new TranslatableString("G-Earth | %s", "tab.extensions.button.logs.windowtitle"));
            stage.initModality(Modality.NONE);
            stage.setAlwaysOnTop(true);
            stage.setMinHeight(235);
            stage.setMinWidth(370);

            Scene scene = new Scene(root);
            scene.getStylesheets().add("/gearth/ui/subforms/extensions/logger/logger.css");
            ExtensionLoggerController controller = loader.getController();
            controller.setStage(stage);

            stage.setScene(scene);
            TitleBarController.create(stage, new GEarthThemedTitleBarConfig(stage) {
                @Override
                public void onCloseClicked() {
                    stage.hide();
                    isVisible = false;
                }
            });

//            // don't let the user close this window on their own
//            stage.setOnCloseRequest(windowEvent -> {
//                stage.hide();
//                isVisible = false;
//            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void show() {
        if (stage != null){
            stage.show();
            if (stage.isIconified()) {
                stage.setIconified(false);
            }
            isVisible = true;
        }
    }

    public void hide() {
        if (stage != null) {
            stage.hide();
            isVisible = false;
        }
    }

    public void log(String s) {
        synchronized (appendOnLoad) {
            if (controller == null) {
                appendOnLoad.add(s);
            }
            else  {
                controller.log(s);
            }
        }
    }

    public boolean isVisible() {
        return isVisible;
    }
}
