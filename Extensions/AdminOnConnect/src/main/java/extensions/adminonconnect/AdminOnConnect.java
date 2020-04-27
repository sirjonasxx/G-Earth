package extensions.adminonconnect;

import gearth.extensions.Extension;
import gearth.extensions.ExtensionInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

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

    protected void initExtension() {
        intercept(HMessage.Direction.TOCLIENT, message -> {
            if (!done) {
                HPacket packet = message.getPacket();
                if (packet.length() == 11) {
                    if (packet.readByte(14) == 0 || packet.readByte(14) == 1) {
                        packet.replaceInt(6, 7);
                        packet.replaceInt(10, 7);
                        packet.replaceBoolean(14, true);

                        done = true;
                        writeToConsole("Replaced user permissions");
                    }
                }
            }
        });

        intercept(HMessage.Direction.TOSERVER, 4000, message -> done = false);
    }

//    protected void onStartConnection() {
//        done = false;
//    }
}
