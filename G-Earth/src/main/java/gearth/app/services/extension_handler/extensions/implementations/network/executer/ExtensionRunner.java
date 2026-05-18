package gearth.app.services.extension_handler.extensions.implementations.network.executer;

import java.io.File;

/**
 * Created by Jonas on 21/09/18.
 */
public interface ExtensionRunner {

    void runAllExtensions(int port);

    void installAndRunExtension(final File path, int port);

    void tryRunExtension(final File path, int port);

    void uninstallExtension(final String path);
}
