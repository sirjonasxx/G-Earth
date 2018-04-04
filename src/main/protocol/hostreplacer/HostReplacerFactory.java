package main.protocol.hostreplacer;

import main.OSValidator;

/**
 * Created by Jonas on 04/04/18.
 */
public class HostReplacerFactory {

    public static HostReplacer get() {

        if (OSValidator.isUnix()) return new LinuxHostReplacer();
        if (OSValidator.isWindows()) return new WindowsHostReplacer();
        if (OSValidator.isMac()) return new MacOSHostReplacer();

        return new LinuxHostReplacer();
    }

}
