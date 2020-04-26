package gearth.ui.extensions;

import gearth.services.extensionhandler.extensions.GEarthExtension;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Created by Jonas on 27/09/18.
 */
public class ExtensionItemContainerProducer {

    private VBox parent;
    private ScrollPane scrollPane;

    private final Object lock = new Object();
    private int port = -1;

    public ExtensionItemContainerProducer(VBox parent, ScrollPane scrollPane) {
        this.parent = parent;
        this.scrollPane = scrollPane;
    }

    void extensionConnected(GEarthExtension extension) {
        synchronized (lock) {
            if (extension.isInstalledExtension()) {
                for (Node n : parent.getChildren()) {
                    if (n instanceof ExtensionItemContainer) {
                        ExtensionItemContainer container = (ExtensionItemContainer) n;
                        if (container.getExtensionFileName() != null && container.getExtensionFileName().equals(extension.getFileName())) {
                            container.hasReconnected(extension);
                            return;
                        }
                    }
                }
            }

            new ExtensionItemContainer(extension, parent, scrollPane, port);
        }
    }

    void setPort(int port) {
        this.port = port;
    }

}
