package gearth.app.protocol.connection.proxy.nitro.os;

import gearth.app.misc.OSValidator;
import gearth.app.protocol.connection.proxy.nitro.os.macos.NitroMacOS;
import gearth.app.protocol.connection.proxy.nitro.os.windows.NitroWindows;

public final class NitroOsFunctionsFactory {

    public static NitroOsFunctions create() {
        if (OSValidator.isWindows()) {
            return new NitroWindows();
        }

        if (OSValidator.isUnix()) {
            throw new UnsupportedOperationException("unix nitro is not implemented yet");
        }

        if (OSValidator.isMac()) {
            return new NitroMacOS();
        }

        throw new UnsupportedOperationException("unsupported operating system");
    }
}
