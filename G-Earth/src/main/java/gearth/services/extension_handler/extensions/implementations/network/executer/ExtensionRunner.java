package gearth.services.extension_handler.extensions.implementations.network.executer;

/**
 * Created by Jonas on 21/09/18.
 */
public interface ExtensionRunner {

    void runAllExtensions(int port);

    void installAndRunExtension(String path, int port);

    void tryRunExtension(String path, int port);

    void uninstallExtension(String path);
}
