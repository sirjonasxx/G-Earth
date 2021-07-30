package gearth.protocol.portchecker;

import gearth.misc.OSValidator;
import org.apache.commons.lang3.NotImplementedException;

public final class PortCheckerFactory {
    private PortCheckerFactory() {}

    public static PortChecker getPortChecker() {
        if (OSValidator.isWindows()) {
            return new WindowsPortChecker();
        }

        if (OSValidator.isUnix()) {
            return new UnixPortChecker();
        }

        throw new NotImplementedException("macOS port checker not implemented yet");
    }
}
