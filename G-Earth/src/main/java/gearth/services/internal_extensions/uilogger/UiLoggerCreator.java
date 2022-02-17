package gearth.services.internal_extensions.uilogger;

import gearth.extensions.InternalExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class UiLoggerCreator extends InternalExtensionFormCreator<UiLogger> {
    @Override
    public UiLogger createForm(Stage stage) throws Exception {
        UiLogger uiLogger = new UiLogger();

        FXMLLoader loader = new FXMLLoader(UiLogger.class.getResource("UiLogger.fxml"));

        Parent root = loader.load();
        stage.setTitle("G-Earth | Packet Logger");
        stage.initModality(Modality.NONE);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/gearth/ui/themes/G-Earth/logoSmall.png")));

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/gearth/ui/themes/G-Earth/styling.css");
        scene.getStylesheets().add("/gearth/services/internal_extensions/uilogger/logger.css");

        UiLoggerController controller = loader.getController();
        uiLogger.setController(controller);
        controller.setStage(stage);

        stage.setScene(scene);
//        TitleBarController.create(stage, new DefaultTitleBarConfig(stage) {
//            @Override
//            public void onCloseClicked() {
//                stage.hide();
////                uiLogger.onHide();
//            }
//        });
        return uiLogger;
    }
}
