package gearth.extensions;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Created by Jonas on 6/11/2018.
 */
public class ExtensionFormLauncher extends Application {

    private static Class<? extends ExtensionForm> extension;
    private static String[] args;

    @Override
    public void start(Stage primaryStage) throws Exception {
        ExtensionInfo extInfo = extension.getAnnotation(ExtensionInfo.class);

        ExtensionForm creator = extension.newInstance();
        ExtensionForm extensionForm = creator.launchForm(primaryStage);

        Extension extension = new Extension(args) {
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
        extensionForm.extension = extension;

        extensionForm.primaryStage = primaryStage;
        Thread t = new Thread(() -> {
            extension.run();
            //when the extension has ended, close this process
            System.exit(0);
        });
        t.start();

        Platform.setImplicitExit(false);

        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            Platform.runLater(() -> {
                primaryStage.hide();
                extensionForm.onHide();
            });
        });
    }

    public static void trigger( Class<? extends ExtensionForm> extension, String[] args) {
        ExtensionFormLauncher.extension = extension;
        ExtensionFormLauncher.args = args;
        launch(args);
    }

}
