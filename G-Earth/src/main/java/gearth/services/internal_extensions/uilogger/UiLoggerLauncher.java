package gearth.services.internal_extensions.uilogger;

import gearth.GEarth;
import gearth.extensions.InternalExtensionFormLauncher;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UiLoggerLauncher extends InternalExtensionFormLauncher<UiLogger> {
    @Override
    public UiLogger createForm(Stage stage) throws Exception {
        UiLogger uiLogger = new UiLogger();

        FXMLLoader loader = new FXMLLoader(UiLogger.class.getResource("UiLogger.fxml"));

        Parent root = loader.load();
        stage.setTitle(String.format("%s | Packet Logger", GEarth.theme));
        stage.initModality(Modality.NONE);
        stage.getIcons().add(new Image(getClass().getResourceAsStream(String.format("/gearth/themes/%s/logoSmall.png", GEarth.theme))));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(String.format("/gearth/themes/%s/styling.css", GEarth.theme));
        scene.getStylesheets().add("/gearth/services/internal_extensions/uilogger/logger.css");

        UiLoggerController controller = loader.getController();
        uiLogger.setController(controller);
        controller.setStage(stage);

        stage.setScene(scene);
        return uiLogger;
    }
}
