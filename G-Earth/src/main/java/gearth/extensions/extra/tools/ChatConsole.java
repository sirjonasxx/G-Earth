package gearth.extensions.extra.tools;

import gearth.extensions.ExtensionInfo;
import gearth.extensions.IExtension;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

/**
 * Created by Jonas on 3/12/2018.
 */

/**
 * Must be created in initextension
 */
public class ChatConsole {

    private volatile int chatid;
    private volatile String name;
    private volatile String infoMessage;

    private volatile boolean firstTime = true;
    private volatile Observable<ChatInputListener> chatInputObservable = new Observable<>();

    private final IExtension extension;


    public ChatConsole(IExtension extension) {
       this(extension, null);
    }

    /**
     * infomessage will be used as response for :info and for initialize
     * @param extension
     * @param infoMessage
     */
    public ChatConsole(IExtension extension, String infoMessage) {
        this.extension = extension;
        this.name = extension.getClass().getAnnotation(ExtensionInfo.class).Title();
        chatid = (this.name.hashCode() % 300000000) + 300000000;
        this.infoMessage = infoMessage;

        final boolean[] doOncePerConnection = {false};
        extension.onConnect((host, port, hotelversion, clientIdentifier, clientType) ->
                doOncePerConnection[0] = true
        );

        extension.intercept(HMessage.Direction.TOSERVER, hMessage -> {
            // if the first packet on init is not 4000, the extension was already running, so we open the chat instantly
            if (firstTime) {
                firstTime = false;
                if (hMessage.getPacket().headerId() != 4000) {
                    doOncePerConnection[0] = false;
                    createChat();
                }
            }
        });

        extension.intercept(HMessage.Direction.TOCLIENT, "FriendListFragment", hMessage -> {
            if (doOncePerConnection[0]) {
                doOncePerConnection[0] = false;

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        createChat();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });

        extension.intercept(HMessage.Direction.TOSERVER, "SendMsg", hMessage -> {
            HPacket packet = hMessage.getPacket();
            if (packet.readInteger() == chatid) {
                hMessage.setBlocked(true);
                String str = packet.readString();
                if (str.equals(":info") && infoMessage != null) {
                    writeOutput(infoMessage, false);
                }
                else {
                    chatInputObservable.fireEvent(l -> l.inputEntered(str));
                }
            }
        });
    }

    private void createChat() {
        HPacket packet = new HPacket("FriendListUpdate", HMessage.Direction.TOCLIENT,
                0, 1, 0, chatid, " [G-Earth] - " + name,
                1, true, false, "ha-1015-64.hd-209-30.cc-260-64.ch-235-64.sh-305-64.lg-285-64",
                0, "", 0, true, false, true, "");

        extension.sendToClient(packet);

        if (infoMessage != null) {
            writeOutput(infoMessage, false);
        }
    }

    public void writeOutput(String string, boolean asInvite) {
        if (asInvite) {
            extension.sendToClient(new HPacket("RoomInvite", HMessage.Direction.TOCLIENT, chatid, string));
        }
        else {
            extension.sendToClient(new HPacket("NewConsole", HMessage.Direction.TOCLIENT, chatid, string, 0, ""));
        }
    }

    public void onInput(ChatInputListener listener) {
        chatInputObservable.addListener(listener);
    }


}
