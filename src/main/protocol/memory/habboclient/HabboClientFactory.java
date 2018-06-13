package main.protocol.memory.habboclient;

import main.misc.OSValidator;
import main.protocol.memory.habboclient.linux.LinuxHabboClient;

/**
 * Created by Jonas on 13/06/18.
 */
public class HabboClientFactory {


    public static HabboClient get() {
        if (OSValidator.isUnix()) return new LinuxHabboClient();

        return null;
    }


}
