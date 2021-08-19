package gearth.services.internal_extensions.extensionstore;

import gearth.Main;
import gearth.extensions.InternalExtensionFormLauncher;
import gearth.services.internal_extensions.extensionstore.application.GExtensionStoreController;
import gearth.ui.GEarthController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class GExtensionStoreLauncher extends InternalExtensionFormLauncher<GExtensionStore> {

    @Override
    public GExtensionStore createForm(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(GExtensionStoreController.class.getResource("gextensionstore.fxml"));
        Parent root = loader.load();

//        stage.getIcons().add(new Image(GExtensionStoreController.class.getResourceAsStream("webview/images/logo.png")));
        stage.setTitle("G-ExtensionStore");
        stage.setMinWidth(420);
        stage.setMinHeight(500);

        stage.setWidth(550);
        stage.setHeight(530);

        stage.setScene(new Scene(root));
        stage.getScene().getStylesheets().add(GEarthController.class.getResource("/gearth/ui/bootstrap3.css").toExternalForm());
        stage.getIcons().add(new Image(Main.class.getResourceAsStream("G-EarthLogoSmaller.png")));

        GExtensionStore gExtensionStore = new GExtensionStore();

        GExtensionStoreController gExtensionStoreController = loader.getController();
        gExtensionStore.setgExtensionStoreController(gExtensionStoreController);
        gExtensionStoreController.gExtensionStore(gExtensionStore);

        return gExtensionStore;
    }
}
