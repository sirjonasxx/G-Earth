package gearth.services.extensionhandler.extensions.implementations.network.executer;

/**
 * Created by Jonas on 22/09/18.
 */
public class ExtensionRunnerFactory {

    private static ExtensionRunner runner = new NormalExtensionRunner();

    public static ExtensionRunner get() {
        return runner;
    }
}
