package gearth.extensions;

import gearth.services.extensionhandler.extensions.GEarthExtension;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducerObserver;
import javafx.application.Platform;
import javafx.stage.Stage;

public class InternalExtensionFormBuilder {

    public static void launch(Class<? extends ExtensionForm> extension, ExtensionProducerObserver observer) {
        try {
            ExtensionInfo extInfo = extension.getAnnotation(ExtensionInfo.class);
            ExtensionForm creator = extension.newInstance();

            Stage stage = new Stage();
            ExtensionForm extensionForm = creator.launchForm(stage);

            InternalExtension internalExtension = new InternalExtension() {
                @Override
                protected void initExtension() {
                    extensionForm.initExtension();
                }

                @Override
                protected void onClick() {
                    extensionForm.onClick();
                }

                @Override
                protected void onStartConnection() {
                    extensionForm.onStartConnection();
                }

                @Override
                protected void onEndConnection() {
                    extensionForm.onEndConnection();
                }

                @Override
                protected ExtensionInfo getInfoAnnotations() {
                    return extInfo;
                }

                @Override
                protected boolean canLeave() {
                    return extensionForm.canLeave();
                }

                @Override
                protected boolean canDelete() {
                    return extensionForm.canDelete();
                }
            };
            extensionForm.extension = internalExtension;
            extensionForm.primaryStage = stage;

            GEarthExtension gEarthExtension = new InternalExtensionBuilder(internalExtension);
            observer.onExtensionProduced(gEarthExtension);


            Platform.setImplicitExit(false);

            stage.setOnCloseRequest(event -> {
                event.consume();
                Platform.runLater(() -> {
                    stage.hide();
                    extensionForm.onHide();
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
