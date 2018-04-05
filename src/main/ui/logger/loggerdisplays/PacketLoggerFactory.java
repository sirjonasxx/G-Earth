package main.ui.logger.loggerdisplays;

/**
 * Created by Jonas on 04/04/18.
 */
public class PacketLoggerFactory {

    public static PacketLogger get() {
        if (System.getenv("XDG_CURRENT_DESKTOP") != null && System.getenv("XDG_CURRENT_DESKTOP").toLowerCase().contains("gnome")) {
            return new GnomeTerminalLogger();
        }
        return new SimpleTerminalLogger();
    }

}
