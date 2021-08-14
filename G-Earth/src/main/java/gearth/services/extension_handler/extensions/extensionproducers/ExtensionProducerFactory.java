package gearth.services.extension_handler.extensions.extensionproducers;

import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionsProducer;
import gearth.services.extension_handler.extensions.implementations.simple.SimpleExtensionProducer;

import java.util.ArrayList;
import java.util.List;

public class ExtensionProducerFactory {
    // returns one of every ExtensionProducer class we have created, to support all types of extensions

    public static List<ExtensionProducer> getAll() {
        List<ExtensionProducer> all = new ArrayList<>();
        all.add(new NetworkExtensionsProducer());
        all.add(new SimpleExtensionProducer());

        return all;
    }


}
