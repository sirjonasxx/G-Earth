package g_earth.extensions.examples.adminonconnect;

import g_earth.extensions.Extension;
import g_earth.extensions.ExtensionInfo;
import g_earth.protocol.HMessage;
import g_earth.protocol.HPacket;

/**
 * Created by Jonas on 26/06/18.
 */



@ExtensionInfo(
        Title = "Always admin!",
        Description = "Gives you admin permission on connect",
        Version = "1.0",
        Author = "sirjonasxx"
)
public class AdminOnConnect extends Extension {

    public static void main(String[] args) {
        new AdminOnConnect(args).run();
    }
    public AdminOnConnect(String[] args) {
        super(args);
    }

    private boolean done = true;

    protected void init() {
        intercept(HMessage.Side.TOCLIENT, message -> {
            if (!done) {
                HPacket packet = message.getPacket();
                if (packet.length() == 11) {
                    if (packet.readByte(14) == 0 || packet.readByte(14) == 1) {
                        packet.replaceInt(6, 7);
                        packet.replaceInt(10, 7);
                        packet.replaceBoolean(14, true);

                        done = true;
                    }
                }
            }
        });
    }

    protected void onStartConnection() {
        done = false;
    }
}
