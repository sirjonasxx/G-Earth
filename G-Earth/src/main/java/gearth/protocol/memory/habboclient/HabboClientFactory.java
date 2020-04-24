package gearth.protocol.memory.habboclient;

import gearth.misc.OSValidator;
import gearth.protocol.HConnection;
import gearth.protocol.memory.habboclient.linux.LinuxHabboClient;
import gearth.protocol.memory.habboclient.macOs.MacOsHabboClient;
import gearth.protocol.memory.habboclient.windows.WindowsHabboClient;

/**
 * Created by Jonas on 13/06/18.
 */
public class HabboClientFactory {


    public static HabboClient get(HConnection connection) {
        if (OSValidator.isUnix()) return new LinuxHabboClient(connection);
        if (OSValidator.isWindows()) return new WindowsHabboClient(connection);
        if (OSValidator.isMac()) return new MacOsHabboClient(connection);

        // todo use rust if beneficial

        return null;
    }


}
