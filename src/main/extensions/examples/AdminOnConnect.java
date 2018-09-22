package main.extensions.examples;

import main.extensions.Extension;
import main.protocol.HMessage;
import main.protocol.HPacket;

/**
 * Created by Jonas on 26/06/18.
 */
public class AdminOnConnect extends Extension {


    public static void main(String[] args) {
        new AdminOnConnect(args);
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

    @Override
    protected void onClick() {
        System.out.println("clicked");
    }

    protected String getTitle() {
        return "Always admin!";
    }
    protected String getDescription() {
        return "Gives you admin permission on connect";
    }
    protected String getVersion() {
        return "1.0";
    }
    protected String getAuthor() {
        return "sirjonasxx";
    }
}
