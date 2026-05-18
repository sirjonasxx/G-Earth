package gearth.extensions;

import gearth.misc.HostInfo;
import gearth.misc.listenerpattern.Observable;
import gearth.misc.listenerpattern.ObservableObject;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.PacketInfoManager;

import java.util.*;

public abstract class ExtensionBase extends IExtension {

    public interface MessageListener {
        void act(HMessage message);
    }
    public interface FlagsCheckListener {
        void act(String[] args);
    }

    protected final Map<Integer, List<MessageListener>> incomingMessageListeners = new HashMap<>();
    protected final Map<Integer, List<MessageListener>> outgoingMessageListeners = new HashMap<>();

    protected final Map<String, List<MessageListener>> hashOrNameIncomingListeners = new HashMap<>();
    protected final Map<String, List<MessageListener>> hashOrNameOutgoingListeners = new HashMap<>();


    volatile PacketInfoManager packetInfoManager = PacketInfoManager.EMPTY;
    ObservableObject<HostInfo> observableHostInfo = new ObservableObject<>(null);

    public void updateHostInfo(HostInfo hostInfo) {
        observableHostInfo.setObject(hostInfo);
    }

    /**
     * Register a listener on a specific packet Type
     * @param direction ToClient or ToServer
     * @param headerId the packet header ID
     * @param messageListener the callback
     */
    public void intercept(HMessage.Direction direction, int headerId, MessageListener messageListener) {
        Map<Integer, List<MessageListener>> listeners =
                direction == HMessage.Direction.TOCLIENT ?
                        incomingMessageListeners :
                        outgoingMessageListeners;

        synchronized (listeners) {
            if (!listeners.containsKey(headerId)) {
                listeners.put(headerId, new ArrayList<>());
            }

            listeners.get(headerId).add(messageListener);
        }
    }

    /**
     * Register a listener on a specific packet Type
     * @param direction ToClient or ToServer
     * @param hashOrName the packet hash or name
     * @param messageListener the callback
     */
    public void intercept(HMessage.Direction direction, String hashOrName, MessageListener messageListener) {
        Map<String, List<MessageListener>> listeners =
                direction == HMessage.Direction.TOCLIENT ?
                        hashOrNameIncomingListeners :
                        hashOrNameOutgoingListeners;

        synchronized (listeners) {
            if (!listeners.containsKey(hashOrName)) {
                listeners.put(hashOrName, new ArrayList<>());
            }

            listeners.get(hashOrName).add(messageListener);
        }
    }

    /**
     * Register a listener on all packets
     * @param direction ToClient or ToServer
     * @param messageListener the callback
     */
    public void intercept(HMessage.Direction direction, MessageListener messageListener) {
        intercept(direction, -1, messageListener);
    }

    @Override
    public void writeToConsole(String s) {
        writeToConsole("black", s);
    }

    protected boolean isOnClickMethodUsed() {
        Class<? extends ExtensionBase> c = getClass();
        while (c != Extension.class) {
            try {
                c.getDeclaredMethod("onClick");
                // if it didnt error, onClick exists
                return true;
            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
            }

            c = (Class<? extends ExtensionBase>) c.getSuperclass();
        }
        return false;
    }

    public void modifyMessage(HMessage habboMessage) {
        HPacket habboPacket = habboMessage.getPacket();

        Map<Integer, List<MessageListener>> listeners =
                habboMessage.getDestination() == HMessage.Direction.TOCLIENT ?
                        incomingMessageListeners :
                        outgoingMessageListeners;

        Map<String, List<MessageListener>> hashOrNameListeners =
                habboMessage.getDestination() == HMessage.Direction.TOCLIENT ?
                        hashOrNameIncomingListeners :
                        hashOrNameOutgoingListeners ;

        Set<MessageListener> correctListeners = new HashSet<>();

        synchronized (listeners) {
            if (listeners.containsKey(-1)) { // registered on all packets
                for (int i = listeners.get(-1).size() - 1; i >= 0; i--) {
                    correctListeners.add(listeners.get(-1).get(i));
                }
            }

            if (listeners.containsKey(habboPacket.headerId())) {
                for (int i = listeners.get(habboPacket.headerId()).size() - 1; i >= 0; i--) {
                    correctListeners.add(listeners.get(habboPacket.headerId()).get(i));
                }
            }
        }

        synchronized (hashOrNameListeners) {
            List<PacketInfo> packetInfos = packetInfoManager.getAllPacketInfoFromHeaderId(habboMessage.getDestination(), habboPacket.headerId());

            List<String> identifiers = new ArrayList<>();
            packetInfos.forEach(packetInfo -> {
                String name = packetInfo.getName();
                String hash = packetInfo.getHash();
                if (name != null && hashOrNameListeners.containsKey(name)) identifiers.add(name);
                if (hash != null && hashOrNameListeners.containsKey(hash)) identifiers.add(hash);
            });

            for (String identifier : identifiers) {
                for (int i = hashOrNameListeners.get(identifier).size() - 1; i >= 0; i--) {
                    correctListeners.add(hashOrNameListeners.get(identifier).get(i));
                }
            }
        }

        for(MessageListener listener : correctListeners) {
            habboMessage.getPacket().resetReadIndex();
            listener.act(habboMessage);
        }
        habboMessage.getPacket().resetReadIndex();
    }

    /**
     * The application got doubleclicked from the G-Earth interface. Doing something here is optional
     */
    @Override
    public void onClick() {}

    @Override
    public ExtensionInfo getInfoAnnotations() {
        return getClass().getAnnotation(ExtensionInfo.class);
    }

    private Observable<OnConnectionListener> onConnectionObservable = new Observable<>();
    public void onConnect(OnConnectionListener listener){
        onConnectionObservable.addListener(listener);
    }

    public Observable<OnConnectionListener> getOnConnectionObservable() {
        return onConnectionObservable;
    }

    public void setPacketInfoManager(PacketInfoManager packetInfoManager) {
        this.packetInfoManager = packetInfoManager;
    }

    @Override
    public PacketInfoManager getPacketInfoManager() {
        return packetInfoManager;
    }

    public HostInfo getHostInfo() {
        return observableHostInfo.getObject();
    }
}
