package gearth.services.extensionhandler;

import gearth.Main;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HState;
import gearth.services.extensionhandler.extensions.ExtensionListener;
import gearth.services.extensionhandler.extensions.GEarthExtension;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducerFactory;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.services.packet_info.PacketInfoManager;
import javafx.util.Pair;

import java.io.IOException;
import java.util.*;

public class ExtensionHandler {

    private final List<GEarthExtension> gEarthExtensions = new ArrayList<>();
    private final HConnection hConnection;
    private List<ExtensionProducer> extensionProducers;
    private Observable<ExtensionConnectedListener> observable = new Observable<ExtensionConnectedListener>() {
        @Override
        public void addListener(ExtensionConnectedListener extensionConnectedListener) {
            super.addListener(extensionConnectedListener);
            for (GEarthExtension gEarthExtension : gEarthExtensions) {
                extensionConnectedListener.onExtensionConnect(gEarthExtension);
            }
        }
    };

    private final Map<HMessage, Set<GEarthExtension>> awaitManipulation = new HashMap<>();
    private final Map<HMessage, OnHMessageHandled> finishManipulationCallback = new HashMap<>();
    private final Map<Pair<HMessage.Direction, Integer>, HMessage> originalMessages = new HashMap<>();
    private final Map<HMessage, HMessage> editedMessages = new HashMap<>();
    private final TreeSet<HMessage> allAwaitingMessages = new TreeSet<>(Comparator.comparingInt(HMessage::getIndex));

    public ExtensionHandler(HConnection hConnection) {
        this.hConnection = hConnection;
        hConnection.setExtensionHandler(this);
        initialize();
    }

    private void initialize() {
        hConnection.getStateObservable().addListener((oldState, newState) -> {
            if (newState == HState.CONNECTED) {
                synchronized (gEarthExtensions) {
                    for (GEarthExtension extension : gEarthExtensions) {
                        extension.connectionStart(
                                hConnection.getDomain(),
                                hConnection.getServerPort(),
                                hConnection.getHotelVersion(),
                                hConnection.getClientIdentifier(),
                                hConnection.getClientType(),
                                hConnection.getPacketInfoManager()
                        );
                    }
                }
            }
            if (oldState == HState.CONNECTED) {
                synchronized (gEarthExtensions) {
                    for (GEarthExtension extension : gEarthExtensions) {
                        extension.connectionEnd();
                    }
                }
            }
        });

        extensionProducers = ExtensionProducerFactory.getAll();
        extensionProducers.forEach(extensionProducer -> extensionProducer.startProducing(createExtensionProducerObserver()));
    }


    private final Object hMessageStuffLock = new Object();
    private void onExtensionRespond(GEarthExtension extension, HMessage edited) {
        HMessage hMessage;

        synchronized (hMessageStuffLock) {
            Pair<HMessage.Direction, Integer> msgDirAndId = new Pair<>(edited.getDestination(), edited.getIndex());
            hMessage = originalMessages.get(msgDirAndId);

            if (awaitManipulation.containsKey(hMessage)) {
                awaitManipulation.get(hMessage).remove(extension);

                boolean wasBlocked = hMessage.isBlocked() ||
                        (editedMessages.get(hMessage) != null && editedMessages.get(hMessage).isBlocked());

                if (!hMessage.equals(edited)) {
                    editedMessages.put(hMessage, edited);
                    if (wasBlocked) {
                        editedMessages.get(hMessage).setBlocked(true);
                    }
                }
                else if (edited.isBlocked()) {
                    editedMessages.putIfAbsent(hMessage, edited);
                    editedMessages.get(hMessage).setBlocked(true);
                }

            }
            else {
                hMessage = null;
            }
        }

        if (hMessage != null) {
            maybeFinishHmessage(hMessage);
        }
    }
    private void onExtensionRemoved(GEarthExtension extension) {
        List<HMessage> awaiting;
        synchronized (hMessageStuffLock) {
            awaiting = new ArrayList<>(allAwaitingMessages);
        }
        for (HMessage hMessage : awaiting) {
            synchronized (hMessageStuffLock) {
                awaitManipulation.get(hMessage).remove(extension);
            }
            maybeFinishHmessage(hMessage);
        }
    }

    // argument is the original hmessage, not an edited one
    private void maybeFinishHmessage(HMessage hMessage) {
        OnHMessageHandled maybeCallback = null;
        HMessage result = null;

        synchronized (hMessageStuffLock) {
            if (hMessage != null && awaitManipulation.containsKey(hMessage)) {
                boolean isFinished = awaitManipulation.get(hMessage).isEmpty();

                if (isFinished) {
                    awaitManipulation.remove(hMessage);
                    result = editedMessages.get(hMessage) == null ? hMessage : editedMessages.get(hMessage);
                    editedMessages.remove(hMessage);
                    originalMessages.remove(new Pair<>(result.getDestination(), result.getIndex()));
                    allAwaitingMessages.remove(hMessage);
                    maybeCallback = finishManipulationCallback.remove(hMessage);
                }
            }
        }

        if (maybeCallback != null) {
            try {
                maybeCallback.finished(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void handle(HMessage hMessage, OnHMessageHandled callback) {
        synchronized (hMessageStuffLock) {
            Pair<HMessage.Direction, Integer> msgDirectionAndId = new Pair<>(hMessage.getDestination(), hMessage.getIndex());
            originalMessages.put(msgDirectionAndId, hMessage);
            finishManipulationCallback.put(hMessage, callback);
            editedMessages.put(hMessage, null);
            allAwaitingMessages.add(hMessage);

            synchronized (gEarthExtensions) {
                awaitManipulation.put(hMessage, new HashSet<>(gEarthExtensions));

                for (GEarthExtension extension : gEarthExtensions) {
                    extension.packetIntercept(new HMessage(hMessage));
                }
            }
        }


        maybeFinishHmessage(hMessage);
    }



    private ExtensionProducerObserver createExtensionProducerObserver() {
        return new ExtensionProducerObserver() {
            @Override
            public void onExtensionProduced(GEarthExtension extension) {
                synchronized (gEarthExtensions) {
                    gEarthExtensions.add(extension);
                }


                ExtensionListener listener = new ExtensionListener() {
                    @Override
                    public void flagsRequest() {
                        extension.provideFlags(Main.args);
                    }

                    @Override
                    public void sendMessage(HMessage.Direction direction, HPacket packet) {
                        if (direction == HMessage.Direction.TOCLIENT) {
                            hConnection.sendToClient(packet);
                        }
                        else {
                            hConnection.sendToServer(packet);
                        }
                    }

                    @Override
                    public void hasClosed() {
                        synchronized (gEarthExtensions) {
                            gEarthExtensions.remove(extension);
                        }
                        onExtensionRemoved(extension);
                        extension.getExtensionObservable().removeListener(this);
                        extension.getDeletedObservable().fireEvent();
                    }

                    @Override
                    protected void manipulatedPacket(HMessage hMessage) {
                        onExtensionRespond(extension, hMessage);
                    }

                    @Override
                    protected void packetToStringRequest(HPacket packet) {
                        String s = "";
                        String expression = "";
                        try {
                            s = packet.toString();
                            if (packet.length() < 3000) {
                                expression = packet.toExpression(hConnection.getPacketInfoManager(), true);
                            }
                        }
                        finally {
                            extension.packetToStringResponse(s, expression);
                        }
                    }

                    @Override
                    protected void stringToPacketRequest(String string) {
                        HPacket packet = new HPacket(string);
                        if (!packet.isPacketComplete()) packet.completePacket(hConnection.getPacketInfoManager());
                        extension.stringToPacketResponse(packet);
                    }
                };

                extension.getExtensionObservable().addListener(listener);

                extension.getRemoveClickObservable().addListener(extension::close);
                extension.getClickedObservable().addListener(extension::doubleclick);
                observable.fireEvent(l -> l.onExtensionConnect(extension));

                extension.init(hConnection.getState() == HState.CONNECTED);
                if (hConnection.getState() == HState.CONNECTED) {
                    extension.connectionStart(
                            hConnection.getDomain(),
                            hConnection.getServerPort(),
                            hConnection.getHotelVersion(),
                            hConnection.getClientIdentifier(),
                            hConnection.getClientType(),
                            hConnection.getPacketInfoManager()
                    );
                }
            }
        };
    }

    public List<ExtensionProducer> getExtensionProducers() {
        return extensionProducers;
    }
    public Observable<ExtensionConnectedListener> getObservable() {
        return observable;
    }

    public void addExtensionProducer(ExtensionProducer producer) {
        producer.startProducing(createExtensionProducerObserver());
        extensionProducers.add(producer);
    }


}
