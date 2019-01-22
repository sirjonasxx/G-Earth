package gearth.extensions.extra.harble;

import gearth.Main;
import gearth.extensions.Extension;
import gearth.extensions.IExtension;
import gearth.misc.harble_api.HarbleAPI;
import gearth.misc.harble_api.HarbleAPIFetcher;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.io.File;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonas on 10/11/2018.
 */
public class HashSupport {

    private final Object lock = new Object();

    private HarbleAPI harbleAPI = new HarbleAPI(""); //empty
    private Map<String, List<Extension.MessageListener>> incomingMessageListeners = new HashMap<>();
    private Map<String, List<Extension.MessageListener>> outgoingMessageListeners = new HashMap<>();

    private IExtension extension;

    public HashSupport(IExtension extension) {
        this.extension = extension;

        extension.onConnect((host, port, hotelversion, cachePath) -> {
//            synchronized (lock) {
            harbleAPI = new HarbleAPI(hotelversion, cachePath);
//            }
        });

        extension.intercept(HMessage.Side.TOSERVER, message -> {
//            synchronized (lock) {
                HarbleAPI.HarbleMessage haMessage = harbleAPI.getHarbleMessageFromHeaderId(HMessage.Side.TOSERVER, message.getPacket().headerId());
                if (haMessage != null) {
                    List<Extension.MessageListener> listeners_hash = outgoingMessageListeners.get(haMessage.getHash());
                    List<Extension.MessageListener> listeners_name = outgoingMessageListeners.get(haMessage.getName());
                    if (listeners_hash != null) {
                        for (Extension.MessageListener listener : listeners_hash) {
                            listener.act(message);
                            message.getPacket().resetReadIndex();
                        }
                    }
                    if (listeners_name != null) {
                        for (Extension.MessageListener listener : listeners_name) {
                            listener.act(message);
                            message.getPacket().resetReadIndex();
                        }
                    }
                }
//            }
        });
        extension.intercept(HMessage.Side.TOCLIENT, message -> {
//            synchronized (lock) {
                HarbleAPI.HarbleMessage haMessage = harbleAPI.getHarbleMessageFromHeaderId(HMessage.Side.TOCLIENT, message.getPacket().headerId());
                if (haMessage != null) {
                    List<Extension.MessageListener> listeners_hash = incomingMessageListeners.get(haMessage.getHash());
                    List<Extension.MessageListener> listeners_name = incomingMessageListeners.get(haMessage.getName());
                    if (listeners_hash != null) {
                        for (Extension.MessageListener listener : listeners_hash) {
                            listener.act(message);
                            message.getPacket().resetReadIndex();
                        }
                    }
                    if (listeners_name != null) {
                        for (Extension.MessageListener listener : listeners_name) {
                            listener.act(message);
                            message.getPacket().resetReadIndex();
                        }
                    }
                }
//            }
        });
    }

    public void intercept(HMessage.Side side, String hashOrName, Extension.MessageListener messageListener) {
        Map<String, List<Extension.MessageListener>> messageListeners =
                (side == HMessage.Side.TOSERVER
                        ? outgoingMessageListeners
                        : incomingMessageListeners);

        messageListeners.computeIfAbsent(hashOrName, k -> new ArrayList<>());
        messageListeners.get(hashOrName).add(messageListener);
    }

    private boolean send(HMessage.Side side, String hashOrName, Object... objects) {
        int headerId;
        HarbleAPI.HarbleMessage fromname = harbleAPI.getHarbleMessageFromName(side, hashOrName);
        if (fromname != null) {
            headerId = fromname.getHeaderId();
        }
        else {
            List<HarbleAPI.HarbleMessage> possibilities = harbleAPI.getHarbleMessagesFromHash(side, hashOrName);
            if (possibilities.size() == 0) return false;
            headerId = possibilities.get(0).getHeaderId();
        }

        try {
            HPacket packetToSend = new HPacket(headerId, objects);

            return (side == HMessage.Side.TOCLIENT
                    ? extension.sendToClient(packetToSend)
                    : extension.sendToServer(packetToSend));
        }
        catch (InvalidParameterException e) {
            return false;
        }
    }

    /**
     *
     * @return if no errors occurred (invalid hash/invalid parameters/connection error)
     */
    public boolean sendToClient(String hashOrName, Object... objects) {
        return send(HMessage.Side.TOCLIENT, hashOrName, objects);
    }

    /**
     *
     * @return if no errors occurred (invalid hash/invalid parameters/connection error)
     */
    public boolean sendToServer(String hashOrName, Object... objects) {
        return send(HMessage.Side.TOSERVER, hashOrName, objects);
    }

}
