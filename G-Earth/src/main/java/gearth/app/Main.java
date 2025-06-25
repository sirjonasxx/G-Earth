package gearth.app;

import gearth.app.misc.Logging;

public class Main {

    public static void main(String[] args) {
        GEarth.args = args;

        // Bridge JUL to SLF4J.
        Logging.bridgeJavaLoggingToSlf4j();
        Logging.setLogLevel(GEarth.hasFlag("--debug"));

        GEarth.launch(GEarth.class, args);
    }

}
