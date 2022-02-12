package gearth.extensions;

import gearth.GEarth;
import gearth.services.extension_handler.extensions.GEarthExtension;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducerObserver;
import javafx.application.Platform;
import javafx.stage.Stage;

public class InternalExtensionFormBuilder<L extends InternalExtensionFormLauncher<T>, T extends ExtensionForm> {

    public T launch(L launcher, ExtensionProducerObserver observer) {
        try {
            Stage stage = new Stage();
            T extensionForm = launcher.createForm(stage);

            ExtensionInfo extInfo = extensionForm.getClass().getAnnotation(ExtensionInfo.class);

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
            extensionForm.hostServices = GEarth.main.getHostServices();
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

            return extensionForm;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
