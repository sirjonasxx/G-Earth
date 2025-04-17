package gearth;

import gearth.misc.Logging;

public class Main {

    public static void main(String[] args) {
        GEarth.args = args;

        Logging.setLogLevel(GEarth.hasFlag("--debug"));

        GEarth.launch(GEarth.class, args);
    }

}
