package gearth.app.protocol.portchecker;

import gearth.app.misc.OSValidator;

public final class PortCheckerFactory {
    private PortCheckerFactory() {}

    public static PortChecker getPortChecker() {
        if (OSValidator.isWindows()) {
            return new WindowsPortChecker();
        }

        if (OSValidator.isUnix()) {
            return new UnixPortChecker();
        }

        throw new UnsupportedOperationException("macOS port checker not implemented yet");
    }
}
