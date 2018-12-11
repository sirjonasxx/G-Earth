package extensions.happyspeech;

import gearth.extensions.Extension;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.extra.harble.HashSupport;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.Random;

/**
 * Created by Jonas on 24/06/18.
 */

/**
 * This is an example extension and is not included in a G-Earth release
 */

@ExtensionInfo(
        Title = "HappySpeech",
        Description = "Example extension for hashSupport",
        Version = "1.0",
        Author = "sirjonasxx"
)
public class HappySpeech extends Extension {

    public static void main(String[] args) {
        new HappySpeech(args).run();
    }
    private HappySpeech(String[] args) {
        super(args);
    }

    private static final String[] COLORS = {"red", "blue", "cyan", "green", "purple"};
    private static final Random r = new Random();

    private HashSupport hashSupport;

    @Override
    protected void initExtension() {
        hashSupport = new HashSupport(this);
        hashSupport.intercept(HMessage.Side.TOSERVER, "RoomUserShout", this::onSendMessage);
        hashSupport.intercept(HMessage.Side.TOSERVER, "RoomUserTalk", this::onSendMessage);
    }

    private void onSendMessage(HMessage message) {
        HPacket packet = message.getPacket();

        String speechtext = packet.readString();

        message.setBlocked(speechtext.equals("blocked"));
        packet.replaceString(6, "@" + COLORS[r.nextInt(COLORS.length)] + "@" + speechtext + " :)");
    }

    @Override
    protected void onClick() {
        //{l}{u:1047}{i:0}{s:text}{i:0}{i:0}{i:0}{i:0}
        hashSupport.sendToClient("RoomUserTalk", 0, "You clicked on this extension!", 0, 0, 0, 0);
    }
}
