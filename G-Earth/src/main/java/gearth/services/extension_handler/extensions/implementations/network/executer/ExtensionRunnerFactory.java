package gearth.services.extension_handler.extensions.implementations.network.executer;

/**
 * Created by Jonas on 22/09/18.
 */
public final class ExtensionRunnerFactory {

    private static final ExtensionRunner runner = new NormalExtensionRunner();

    public static ExtensionRunner get() {
        return runner;
    }
}
