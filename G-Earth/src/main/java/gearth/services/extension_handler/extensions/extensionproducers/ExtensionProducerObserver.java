package gearth.services.extension_handler.extensions.extensionproducers;

import gearth.services.extension_handler.extensions.GEarthExtension;

public interface ExtensionProducerObserver {
    void onExtensionProduced(GEarthExtension extension);
}
