package gearth.protocol.connection.proxy.nitro.os;

import gearth.misc.OSValidator;
import gearth.protocol.connection.proxy.nitro.os.windows.NitroWindows;
import org.apache.commons.lang3.NotImplementedException;

public final class NitroOsFunctionsFactory {

    public static NitroOsFunctions create() {
        if (OSValidator.isWindows()) {
            return new NitroWindows();
        }

        if (OSValidator.isUnix()) {
            throw new NotImplementedException("unix nitro is not implemented yet");
        }

        throw new NotImplementedException("macOS nitro is not implemented yet");
    }

}
