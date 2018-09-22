package main.ui.extensions.executer;

import main.misc.OSValidator;

/**
 * Created by Jonas on 22/09/18.
 */
public class ExtensionRunnerFactory {

    public static ExtensionRunner get() {

        return new NormalExtensionRunner();
    }
}
