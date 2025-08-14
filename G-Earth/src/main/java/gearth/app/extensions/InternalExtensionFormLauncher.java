package gearth.app.extensions;

import gearth.app.GEarth;
import gearth.app.services.extension_handler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.services.extension_handler.extensions.GEarthExtension;
import javafx.application.Platform;
import javafx.stage.Stage;

public class InternalExtensionFormLauncher<L extends InternalExtensionFormCreator<T>, T extends ExtensionForm> {

    public T launch(L launcher, ExtensionProducerObserver observer) {
        try {
            Stage stage = new Stage();
            T extensionForm = launcher.createForm(stage);

            ExtensionInfo extInfo = extensionForm.getClass().getAnnotation(ExtensionInfo.class);

            InternalExtension internalExtension = new InternalExtension() {
                @Override
                public void initExtension() {
                    extensionForm.initExtension();
                }

                @Override
                public void onClick() {
                    extensionForm.onClick();
                }

                @Override
                public void onStartConnection() {
                    extensionForm.onStartConnection();
                }

                @Override
                public void onEndConnection() {
                    extensionForm.onEndConnection();
                }

                @Override
                public ExtensionInfo getInfoAnnotations() {
                    return extInfo;
                }

                @Override
                public boolean canLeave() {
                    return extensionForm.canLeave();
                }

                @Override
                public boolean canDelete() {
                    return extensionForm.canDelete();
                }
            };
            extensionForm.hostServices = GEarth.main.getHostServices();
            extensionForm.extension = internalExtension;
            extensionForm.primaryStage = stage;

            extensionForm.fieldsInitialized.fireEvent();
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
