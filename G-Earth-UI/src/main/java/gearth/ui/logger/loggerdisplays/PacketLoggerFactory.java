package gearth.ui.logger.loggerdisplays;

import gearth.misc.OSValidator;

/**
 * Created by Jonas on 04/04/18.
 */
public class PacketLoggerFactory {

    public static PacketLogger get() {
        if (OSValidator.isUnix()) {
            return new LinuxTerminalLogger();
        }
//        if (System.getenv("XDG_CURRENT_DESKTOP") != null && System.getenv("XDG_CURRENT_DESKTOP").toLowerCase().contains("gnome")) {
//            return new GnomeTerminalLogger();
//        }
        return new SimpleTerminalLogger();
    }

}
