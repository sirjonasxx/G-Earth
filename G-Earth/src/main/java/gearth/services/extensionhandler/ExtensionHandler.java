package gearth.services.extensionhandler;

import gearth.Main;
import gearth.misc.harble_api.HarbleAPIFetcher;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HState;
import gearth.services.extensionhandler.extensions.ExtensionListener;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducerFactory;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.services.extensionhandler.extensions.GEarthExtension;

import java.util.*;
import java.util.function.Consumer;

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


    public ExtensionHandler(HConnection hConnection) {
        this.hConnection = hConnection;
        initialize();
    }

    private void initialize() {

        hConnection.getStateObservable().addListener((oldState, newState) -> {
            if (newState == HState.CONNECTED) {
                HarbleAPIFetcher.fetch(hConnection.getHotelVersion());
                synchronized (gEarthExtensions) {
                    for (GEarthExtension extension : gEarthExtensions) {
                        extension.connectionStart(
                                hConnection.getDomain(),
                                hConnection.getServerPort(),
                                hConnection.getHotelVersion(),
                                HarbleAPIFetcher.HARBLEAPI == null ? "null" : HarbleAPIFetcher.HARBLEAPI.getPath()
                        );
                    }
                }
            }
            if (oldState == HState.CONNECTED) {
                synchronized (hConnection) {
                    for (GEarthExtension extension : gEarthExtensions) {
                        extension.connectionEnd();
                    }
                }
            }
        });


        hConnection.addTrafficListener(1, message -> {
            Set<GEarthExtension> collection;
            synchronized (gEarthExtensions) {
                collection = new HashSet<>(gEarthExtensions);
            }
            HMessage result = new HMessage(message);

            boolean[] isblock = new boolean[1];
            synchronized (collection) {
                for (GEarthExtension extension : collection) {
                    ExtensionListener respondCallback = new ExtensionListener() {
                        @Override
                        public void manipulatedPacket(HMessage responseMessage) {
                            if (responseMessage.getDestination() == message.getDestination() && responseMessage.getIndex() == message.getIndex()) {
                                synchronized (result) {
                                    if (!message.equals(responseMessage)) {
                                        result.constructFromHMessage(responseMessage);
                                    }
                                    if (responseMessage.isBlocked()) {
                                        isblock[0] = true;
                                    }
                                    synchronized (collection) {
                                        collection.remove(extension);
                                    }

                                    synchronized (extension) {
                                        extension.getExtensionObservable().removeListener(this);
                                    }
                                }
                            }
                        }
                    };
                    synchronized (extension) {
                        extension.getExtensionObservable().addListener(respondCallback);
                    }
                }
            }

            Set<GEarthExtension> collection2;
            synchronized (collection) {
                collection2 = new HashSet<>(collection);
            }

            synchronized (collection2) {
                for (GEarthExtension extension : collection2) {
                    synchronized (extension) {
                        extension.packetIntercept(new HMessage(message));
                    }
                }
            }

            //block untill all extensions have responded
            List<GEarthExtension> willdelete = new ArrayList<>();
            while (true) {
                synchronized (collection) {
                    if (collection.isEmpty()) {
                        break;
                    }

                    synchronized (gEarthExtensions) {
                        for (GEarthExtension extension : collection) {
                            if (!gEarthExtensions.contains(extension)) willdelete.add(extension);
                        }
                    }

                    for (int i = willdelete.size() - 1; i >= 0; i--) {
                        collection.remove(willdelete.get(i));
                        willdelete.remove(i);
                    }
                }


                try {Thread.sleep(1);} catch (InterruptedException e) {e.printStackTrace();}
            }

            message.constructFromHMessage(result);

            if (isblock[0]) {
                message.setBlocked(true);
            }
        });

        extensionProducers = ExtensionProducerFactory.getAll();
        extensionProducers.forEach(this::initializeExtensionProducer);
    }

    private void initializeExtensionProducer(ExtensionProducer producer) {
        producer.startProducing(new ExtensionProducerObserver() {
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
                            hConnection.sendToClientAsync(packet);
                        }
                        else {
                            hConnection.sendToServerAsync(packet);
                        }
                    }

                    @Override
                    public void hasClosed() {
                        synchronized (gEarthExtensions) {
                            gEarthExtensions.remove(extension);
                        }
                        extension.getExtensionObservable().removeListener(this);
                        extension.getDeletedObservable().fireEvent();
                    }
                };

                extension.getExtensionObservable().addListener(listener);
                extension.init();

                if (hConnection.getState() == HState.CONNECTED) {
                    extension.connectionStart(
                            hConnection.getDomain(),
                            hConnection.getServerPort(),
                            hConnection.getHotelVersion(),
                            HarbleAPIFetcher.HARBLEAPI == null ? "null" : HarbleAPIFetcher.HARBLEAPI.getPath()
                    );
                }

                extension.getRemoveClickObservable().addListener(extension::close);
                extension.getClickedObservable().addListener(extension::doubleclick);

                observable.fireEvent(l -> l.onExtensionConnect(extension));
            }
        });
    }


    public List<ExtensionProducer> getExtensionProducers() {
        return extensionProducers;
    }

    public Observable<ExtensionConnectedListener> getObservable() {
        return observable;
    }


}
