package gearth.extensions;

import javafx.stage.Stage;

public abstract class InternalExtensionFormCreator<T extends ExtensionForm> {

    // creates an ExtensionForm object and initializes the JavaFX application
    public abstract T createForm(Stage primaryStage) throws Exception;

}
