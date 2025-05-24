package gearth.app.services.extension_handler;

import gearth.services.extension_handler.extensions.GEarthExtension;

public interface ExtensionConnectedListener {
    void onExtensionConnect(GEarthExtension e);
}
