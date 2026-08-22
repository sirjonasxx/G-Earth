package gearth.app;

import gearth.app.misc.Logging;
import gearth.app.services.extension_handler.extensions.implementations.network.executer.ExtensionJavaRuntime;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && ExtensionJavaRuntime.CHECK_ARGUMENT.equals(args[0])) {
            try {
                ExtensionJavaRuntime.RuntimeInfo runtime = ExtensionJavaRuntime.resolve(
                        ExtensionJavaRuntime.applicationDirectory(Main.class)
                );
                System.out.println(runtime.describe());
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(2);
            }
            return;
        }

        GEarth.args = args;

        // Bridge JUL to SLF4J.
        Logging.bridgeJavaLoggingToSlf4j();
        Logging.setLogLevel(GEarth.hasFlag("--debug"));

        GEarth.launch(GEarth.class, args);
    }

}
