package gearth.app.protocol.memory.habboclient;

import gearth.app.misc.OSValidator;
import gearth.app.protocol.HConnection;
import gearth.app.protocol.memory.habboclient.linux.LinuxHabboClient;
import gearth.app.protocol.memory.habboclient.external.MemoryClient;

/**
 * Created by Jonas on 13/06/18.
 */
public class HabboClientFactory {

    public static HabboClient get(HConnection connection) {
        if (OSValidator.isUnix()) {
            return new LinuxHabboClient(connection);
        }

        return new MemoryClient(connection);
    }

}
