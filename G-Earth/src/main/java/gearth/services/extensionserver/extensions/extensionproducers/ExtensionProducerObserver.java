package gearth.services.extensionserver.extensions.extensionproducers;

import gearth.services.extensionserver.extensions.GEarthExtension;
import gearth.services.extensionserver.extensions.network.NetworkExtension;

public interface ExtensionProducerObserver {
    void onExtensionConnect(GEarthExtension extension);
}
