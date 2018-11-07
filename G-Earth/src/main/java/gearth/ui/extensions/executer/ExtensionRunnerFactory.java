package gearth.ui.extensions.executer;

/**
 * Created by Jonas on 22/09/18.
 */
public class ExtensionRunnerFactory {

    private static ExtensionRunner runner = obtain();

    public static ExtensionRunner get() {
        return runner;
    }

    private static ExtensionRunner obtain() {
        return new NormalExtensionRunner();
    }
}
