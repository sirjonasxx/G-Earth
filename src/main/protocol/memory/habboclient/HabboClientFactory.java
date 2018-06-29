package main.protocol.memory.habboclient;

import main.misc.OSValidator;
import main.protocol.HConnection;
import main.protocol.memory.habboclient.linux.LinuxHabboClient;
import main.protocol.memory.habboclient.windows.WindowsHabboClient;

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
