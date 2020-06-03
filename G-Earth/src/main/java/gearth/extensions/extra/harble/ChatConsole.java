package gearth.extensions.extra.harble;

import gearth.extensions.ExtensionInfo;
import gearth.extensions.IExtension;
import gearth.extensions.OnConnectionListener;
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
    private volatile HashSupport hashSupport;
    private volatile String infoMessage;

    private volatile boolean firstTime = true;
    private volatile Observable<ChatInputListener> chatInputObservable = new Observable<>();


    public ChatConsole(final HashSupport hashSupport, IExtension extension) {
       this(hashSupport, extension, null);
    }

    /**
     * infomessage will be used as response for :info and for initialize
     * @param hashSupport
     * @param extension
     * @param infoMessage
     */
    public ChatConsole(final HashSupport hashSupport, IExtension extension, String infoMessage) {
        this.hashSupport = hashSupport;
        this.name = extension.getClass().getAnnotation(ExtensionInfo.class).Title();
        chatid = (this.name.hashCode() % 300000000) + 300000000;
        this.infoMessage = infoMessage;

        final boolean[] doOncePerConnection = {false};
        extension.onConnect((s, i, s1, h1) -> doOncePerConnection[0] = true);

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

        hashSupport.intercept(HMessage.Direction.TOCLIENT, "Friends", hMessage -> {
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

        hashSupport.intercept(HMessage.Direction.TOSERVER, "FriendPrivateMessage", hMessage -> {
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
        hashSupport.sendToClient("UpdateFriend",
                0, 1, 0, chatid, " [G-Earth] - " + name,
                1, true, false, "ha-1015-64.hd-209-30.cc-260-64.ch-235-64.sh-305-64.lg-285-64",
                0, "", 0, true, false, true, ""
        );

        if (infoMessage != null) {
            writeOutput(infoMessage, false);
        }
    }

    public void writeOutput(String string, boolean asInvite) {
        if (asInvite) {
            hashSupport.sendToClient("ReceiveInvitation", chatid, string);
        }
        else {
            hashSupport.sendToClient("ReceivePrivateMessage", chatid, string, 0, "");
        }
    }

    public void onInput(ChatInputListener listener) {
        chatInputObservable.addListener(listener);
    }


}
