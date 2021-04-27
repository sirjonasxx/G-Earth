package gearth.extensions;

import gearth.services.extensionhandler.extensions.GEarthExtension;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducerObserver;
import javafx.application.Platform;
import javafx.stage.Stage;

public class InternalExtensionFormBuilder<T extends ExtensionForm> {

    public T launch(Class<T> extensionClass, ExtensionProducerObserver observer) {
        try {
            ExtensionInfo extInfo = extensionClass.getAnnotation(ExtensionInfo.class);
            T creator = extensionClass.newInstance();

            Stage stage = new Stage();
            T extensionForm = (T)(creator.launchForm(stage));

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

            return extensionForm;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
