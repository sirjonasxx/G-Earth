package gearth.services.extension_handler.extensions.extensionproducers;

import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionServer;
import gearth.services.extension_handler.extensions.implementations.simple.SimpleExtensionProducer;

import java.util.ArrayList;
import java.util.List;

public class ExtensionProducerFactory {
    // returns one of every ExtensionProducer class we have created, to support all types of extensions

    private final static NetworkExtensionServer EXTENSION_SERVER = new NetworkExtensionServer();

    public static List<ExtensionProducer> getAll() {
        List<ExtensionProducer> all = new ArrayList<>();
        all.add(EXTENSION_SERVER);
        all.add(new SimpleExtensionProducer());

        return all;
    }

    public static NetworkExtensionServer getExtensionServer() {
        return EXTENSION_SERVER;
    }
}
