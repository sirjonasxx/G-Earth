package gearth.extensions.extra.tools;

import gearth.extensions.Extension;
import gearth.extensions.IExtension;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.security.InvalidParameterException;
import java.util.*;

/**
 * Created by Jonas on 10/11/2018.
 */
public class PacketInfoSupport {

    private final Object lock = new Object();

    private PacketInfoManager packetInfoManager = new PacketInfoManager(new ArrayList<>()); //empty
    private Map<String, List<Extension.MessageListener>> incomingMessageListeners = new HashMap<>();
    private Map<String, List<Extension.MessageListener>> outgoingMessageListeners = new HashMap<>();

    private IExtension extension;

    public PacketInfoSupport(IExtension extension) {
        this.extension = extension;

        extension.onConnect((host, port, hotelversion, clientIdentifier, clientType, packetInfoManager) ->
                this.packetInfoManager = packetInfoManager
        );

        extension.intercept(HMessage.Direction.TOSERVER, message -> onReceivePacket(HMessage.Direction.TOSERVER, message));
        extension.intercept(HMessage.Direction.TOCLIENT, message -> onReceivePacket(HMessage.Direction.TOCLIENT, message));
    }

    private void onReceivePacket(HMessage.Direction direction, HMessage message) {
        Set<Extension.MessageListener> callbacks = new HashSet<>();
        Map<String, List<Extension.MessageListener>> messageListeners =
                (direction == HMessage.Direction.TOSERVER
                        ? outgoingMessageListeners
                        : incomingMessageListeners);

        List<PacketInfo> packetInfos = packetInfoManager.getAllPacketInfoFromHeaderId(direction, message.getPacket().headerId());

        for (PacketInfo packetInfo : packetInfos) {
            List<Extension.MessageListener> listeners_hash = messageListeners.get(packetInfo.getHash());
            List<Extension.MessageListener> listeners_name = messageListeners.get(packetInfo.getName());
            if (listeners_hash != null) {
                callbacks.addAll(listeners_hash);
            }
            if (listeners_name != null) {
                callbacks.addAll(listeners_name);
            }
        }

        for (Extension.MessageListener listener : callbacks) {
            listener.act(message);
            message.getPacket().resetReadIndex();
        }
    }

    public void intercept(HMessage.Direction direction, String hashOrName, Extension.MessageListener messageListener) {
        Map<String, List<Extension.MessageListener>> messageListeners =
                (direction == HMessage.Direction.TOSERVER
                        ? outgoingMessageListeners
                        : incomingMessageListeners);

        messageListeners.computeIfAbsent(hashOrName, k -> new ArrayList<>());
        messageListeners.get(hashOrName).add(messageListener);
    }

    private boolean send(HMessage.Direction direction, String hashOrName, Object... objects) {
        int headerId;
        PacketInfo fromname = packetInfoManager.getPacketInfoFromName(direction, hashOrName);
        if (fromname != null) {
            headerId = fromname.getHeaderId();
        }
        else {
            PacketInfo fromHash = packetInfoManager.getPacketInfoFromHash(direction, hashOrName);
            if (fromHash == null) return false;
            headerId = fromHash.getHeaderId();
        }

        try {
            HPacket packetToSend = new HPacket(headerId, objects);

            return (direction == HMessage.Direction.TOCLIENT
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
        return send(HMessage.Direction.TOCLIENT, hashOrName, objects);
    }

    /**
     *
     * @return if no errors occurred (invalid hash/invalid parameters/connection error)
     */
    public boolean sendToServer(String hashOrName, Object... objects) {
        return send(HMessage.Direction.TOSERVER, hashOrName, objects);
    }

    public PacketInfoManager getPacketInfoManager() {
        return packetInfoManager;
    }
}
