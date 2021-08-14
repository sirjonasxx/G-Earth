package gearth.extensions;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Created by Jonas on 6/11/2018.
 */
public class ExtensionFormLauncher extends Application {

    private static Class<? extends ExtensionFormCreator> extensionFormCreator;
    private static String[] args;

    @Override
    public void start(Stage primaryStage) throws Exception {
//        ExtensionInfo extInfo = extensionFormCreator.getAnnotation(ExtensionInfo.class);

        ExtensionFormCreator creator = extensionFormCreator.newInstance();
        ExtensionForm extensionForm = creator.createForm(primaryStage);
        ExtensionInfo extInfo = extensionForm.getClass().getAnnotation(ExtensionInfo.class);

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
        extensionForm.hostServices = getHostServices();
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

    public static void trigger(Class<? extends ExtensionFormCreator> creator, String[] args) {
        ExtensionFormLauncher.extensionFormCreator = creator;
        ExtensionFormLauncher.args = args;
        launch(args);
    }

}
