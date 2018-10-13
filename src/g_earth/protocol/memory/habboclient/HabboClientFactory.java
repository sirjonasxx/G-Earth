package g_earth.protocol.memory.habboclient;

import g_earth.misc.OSValidator;
import g_earth.protocol.HConnection;
import g_earth.protocol.memory.habboclient.linux.LinuxHabboClient;
import g_earth.protocol.memory.habboclient.windows.WindowsHabboClient;

/**
 * Created by Jonas on 13/06/18.
 */
public class HabboClientFactory {


    public static HabboClient get(HConnection connection) {
        if (OSValidator.isUnix()) return new LinuxHabboClient(connection);
        if (OSValidator.isWindows()) return new WindowsHabboClient(connection);

        return null;
    }


}
