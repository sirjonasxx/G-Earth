package gearth.services.extensionhandler.extensions.extensionproducers;

import gearth.services.extensionhandler.extensions.network.NetworkExtensionsProducer;
import gearth.services.extensionhandler.extensions.simple.SimpleExtensionProducer;

import java.util.Arrays;
import java.util.List;

public class ExtensionProducerFactory {
    // returns one of every ExtensionProducer class we have created, to support all types of extensions

    public static List<ExtensionProducer> getAll() {
        return Arrays.asList(
                new NetworkExtensionsProducer(),
                new SimpleExtensionProducer()
        );
    }


}
