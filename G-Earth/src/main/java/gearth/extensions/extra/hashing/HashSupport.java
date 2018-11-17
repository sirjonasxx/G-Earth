package gearth.extensions.extra.hashing;

import gearth.extensions.Extension;
import gearth.misc.harble_api.HarbleAPI;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

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
    private PacketSender toClientSender;
    private PacketSender toServerSender;


    public interface OnConnectRegistration {
        void onConnect(Extension.OnConnectionListener listener);
    }
    public interface InterceptRegistration {
        void intercept(HMessage.Side side, Extension.MessageListener messageListener);
    }
    public interface PacketSender {
        boolean send(HPacket packet);
    }

    public HashSupport(OnConnectRegistration onConnectRegistration, InterceptRegistration interceptRegistration, PacketSender sendToClient, PacketSender sendToServer) {
        toClientSender = sendToClient;
        toServerSender = sendToServer;
        onConnectRegistration.onConnect((host, port, hotelversion) -> {
//            synchronized (lock) {
                harbleAPI = new HarbleAPI(hotelversion);
//            }
        });

        interceptRegistration.intercept(HMessage.Side.TOSERVER, message -> {
//            synchronized (lock) {
                HarbleAPI.HarbleMessage haMessage = harbleAPI.getHarbleMessageFromHeaderId(HMessage.Side.TOSERVER, message.getPacket().headerId());
                if (haMessage != null) {
                    String hash = haMessage.getHash();
                    List<Extension.MessageListener> listeners = outgoingMessageListeners.get(hash);
                    if (listeners != null) {
                        for (Extension.MessageListener listener : listeners) {
                            listener.act(message);
                            message.getPacket().resetReadIndex();
                        }
                    }
                }
//            }
        });
        interceptRegistration.intercept(HMessage.Side.TOCLIENT, message -> {
//            synchronized (lock) {
                HarbleAPI.HarbleMessage haMessage = harbleAPI.getHarbleMessageFromHeaderId(HMessage.Side.TOCLIENT, message.getPacket().headerId());
                if (haMessage != null) {
                    String hash = haMessage.getHash();
                    List<Extension.MessageListener> listeners = incomingMessageListeners.get(hash);
                    if (listeners != null) {
                        for (Extension.MessageListener listener : listeners) {
                            listener.act(message);
                            message.getPacket().resetReadIndex();
                        }
                    }
                }
//            }
        });
    }

    public void intercept(HMessage.Side side, String hash, Extension.MessageListener messageListener) {
        Map<String, List<Extension.MessageListener>> messageListeners =
                (side == HMessage.Side.TOSERVER
                        ? outgoingMessageListeners
                        : incomingMessageListeners);

        messageListeners.computeIfAbsent(hash, k -> new ArrayList<>());
        messageListeners.get(hash).add(messageListener);
    }

    /**
     *
     * @return if no errors occurred (invalid hash/invalid parameters/connection error)
     */
    public boolean sendToClient(String hash, Object... objects) {
        List<HarbleAPI.HarbleMessage> possibilities = harbleAPI.getHarbleMessagesFromHash(HMessage.Side.TOCLIENT, hash);
        if (possibilities.size() == 0) return false;
        int headerId = possibilities.get(0).getHeaderId();

        try {
            HPacket packetToSend = new HPacket(headerId, objects);

            return toClientSender.send(packetToSend);
        }
        catch (InvalidParameterException e) {
            return false;
        }
    }

    /**
     *
     * @return if no errors occurred (invalid hash/invalid parameters/connection error)
     */
    public boolean sendToServer(String hash, Object... objects) {
        List<HarbleAPI.HarbleMessage> possibilities = harbleAPI.getHarbleMessagesFromHash(HMessage.Side.TOSERVER, hash);
        if (possibilities.size() == 0) return false;
        int headerId = possibilities.get(0).getHeaderId();

        try {
            HPacket packetToSend = new HPacket(headerId, objects);

            return toServerSender.send(packetToSend);
        }
        catch (InvalidParameterException e) {
            return false;
        }
    }

}
