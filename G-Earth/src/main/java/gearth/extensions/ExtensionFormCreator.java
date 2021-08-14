package gearth.extensions;

import javafx.stage.Stage;

public abstract class ExtensionFormCreator {

    // creates an ExtensionForm object and initializes the JavaFX application
    protected abstract ExtensionForm createForm(Stage primaryStage) throws Exception;

    public static void runExtensionForm(String[] args, Class<? extends ExtensionFormCreator> creator) {
        ExtensionFormLauncher.trigger(creator, args);
    }

}
