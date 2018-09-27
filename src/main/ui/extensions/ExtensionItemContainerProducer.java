package main.ui.extensions;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Created by Jonas on 27/09/18.
 */
public class ExtensionItemContainerProducer {

    private VBox parent;
    private ScrollPane scrollPane;

    public ExtensionItemContainerProducer(VBox parent, ScrollPane scrollPane) {
        this.parent = parent;
        this.scrollPane = scrollPane;
    }



}
