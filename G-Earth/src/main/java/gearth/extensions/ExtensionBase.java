package gearth.extensions;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ExtensionBase extends IExtension {

    public interface MessageListener {
        void act(HMessage message);
    }
    public interface FlagsCheckListener {
        void act(String[] args);
    }

    protected final Map<Integer, List<MessageListener>> incomingMessageListeners = new HashMap<>();
    protected final Map<Integer, List<MessageListener>> outgoingMessageListeners = new HashMap<>();

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
        }


        listeners.get(headerId).add(messageListener);
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

        List<MessageListener> correctListeners = new ArrayList<>();

        synchronized (incomingMessageListeners) {
            synchronized (outgoingMessageListeners) {
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
    void onClick() {

    }

    @Override
    protected ExtensionInfo getInfoAnnotations() {
        return getClass().getAnnotation(ExtensionInfo.class);
    }

    private Observable<OnConnectionListener> onConnectionObservable = new Observable<>();
    public void onConnect(OnConnectionListener listener){
        onConnectionObservable.addListener(listener);
    }

    Observable<OnConnectionListener> getOnConnectionObservable() {
        return onConnectionObservable;
    }
}
