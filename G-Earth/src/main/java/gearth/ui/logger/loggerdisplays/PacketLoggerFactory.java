package gearth.ui.logger.loggerdisplays;

import gearth.Main;
import gearth.misc.OSValidator;
import gearth.ui.logger.loggerdisplays.uilogger.UiLogger;

/**
 * Created by Jonas on 04/04/18.
 */
public class PacketLoggerFactory {

    public static boolean usesUIlogger() {
        return (!Main.hasFlag("-t"));
    }

    public static PacketLogger get() {
        if (usesUIlogger()) {
            return new UiLogger();
        }

        if (OSValidator.isUnix()) {
            return new LinuxTerminalLogger();
        }
        return new SimpleTerminalLogger();
    }

}
