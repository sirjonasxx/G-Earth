package main.extensions;

import main.protocol.HMessage;
import main.protocol.HPacket;

/**
 * Created by Jonas on 24/06/18.
 */
public class SimpleTestExtension extends Extension {

    public static void main(String[] args) {
        new SimpleTestExtension(args);
    }

    private SimpleTestExtension(String[] args) {
        super(args);
    }

    @Override
    protected void init() {
        System.out.println("init");

        intercept(HMessage.Side.TOSERVER, 1926, this::onSendMessage);
    }

    private void onSendMessage(HMessage message) {
        HPacket packet = message.getPacket();

        String watchasaid = packet.readString();

        System.out.println("you said: " + watchasaid);

        if (watchasaid.equals("blocked")) {
            message.setBlocked(true);
        }

        packet.replaceString(6, "@cyan@" + watchasaid);
    }

    @Override
    protected void onDoubleClick() {
        System.out.println("doubleclick");
    }

    @Override
    protected void onStartConnection() {
        System.out.println("connection started");
    }

    @Override
    protected void onEndConnection() {
        System.out.println("connection ended");
    }

    @Override
    protected String getTitle() {
        return "Simple Test!";
    }

    @Override
    protected String getDescription() {
        return "But just for testing purpose";
    }

    @Override
    protected String getVersion() {
        return "0.1";
    }

    @Override
    protected String getAuthor() {
        return "sirjonasxx";
    }
}
