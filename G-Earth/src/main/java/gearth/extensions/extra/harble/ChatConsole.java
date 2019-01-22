package gearth.extensions.extra.harble;

import gearth.extensions.ExtensionInfo;
import gearth.extensions.IExtension;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;

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
        chatid = this.name.hashCode();
        this.infoMessage = infoMessage;

        final boolean[] doOncePerConnection = {false};

        extension.onConnect((s, i, s1, h1) -> doOncePerConnection[0] = true);

        extension.intercept(HMessage.Side.TOSERVER, hMessage -> {
            // if the first packet on init is not 4000, the extension was already running, so we open the chat instantly
            if (firstTime) {
                firstTime = false;
                if (hMessage.getPacket().headerId() != 4000) {
                    doOncePerConnection[0] = false;
                    createChat();
                }
            }
        });

        hashSupport.intercept(HMessage.Side.TOCLIENT, "Friends", hMessage -> {
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

        hashSupport.intercept(HMessage.Side.TOSERVER, "FriendPrivateMessage", hMessage -> {
            HPacket packet = hMessage.getPacket();
            if (packet.readInteger() == chatid) {
                hMessage.setBlocked(true);
                String str = packet.readString();
                if (str.equals(":info") && infoMessage != null) {
                    writeOutput(infoMessage, false);
                }
                else {
                    notifyChatInputListeners(str);
                }
            }
        });
    }

    private void createChat() {
        hashSupport.sendToClient("UpdateFriend",
                0, 1, false, false, "", chatid, " [G-Earth] - " + name, 1, true, false, "", 0, "", 0, true, true, true, ""
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

    public interface ChatInputListener {
        void inputEntered(String input);
    }
    private List<ChatInputListener> chatInputListenerList = new ArrayList<ChatInputListener>();
    public void onInput(ChatInputListener listener) {
        chatInputListenerList.add(listener);
    }
    private void notifyChatInputListeners (String s) {
        for (ChatInputListener listener : chatInputListenerList) {
            listener.inputEntered(s);
        }
    }


}
