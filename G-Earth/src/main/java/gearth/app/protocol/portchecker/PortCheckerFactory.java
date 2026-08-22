package gearth.app.protocol.portchecker;

import gearth.app.misc.OSValidator;
import org.apache.commons.lang3.NotImplementedException;

public final class PortCheckerFactory {
    private PortCheckerFactory() {}

    public static PortChecker getPortChecker() {
        if (OSValidator.isWindows()) {
            return new WindowsPortChecker();
        }

        if (OSValidator.isMac()) {
            return new MacOsPortChecker();
        }

        if (OSValidator.isUnix()) {
            return new UnixPortChecker();
        }

        throw new NotImplementedException("Port checker not implemented for this operating system");
    }
}
