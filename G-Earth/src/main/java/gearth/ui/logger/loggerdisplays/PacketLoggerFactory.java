package gearth.ui.logger.loggerdisplays;

import gearth.Main;
import gearth.misc.OSValidator;

/**
 * Created by Jonas on 04/04/18.
 */
public class PacketLoggerFactory {

    public static PacketLogger get() {
        if (OSValidator.isUnix() && Main.hasFlag("-t")) {
            return new LinuxTerminalLogger();
        }

        return new UiLogger();
    }

}
