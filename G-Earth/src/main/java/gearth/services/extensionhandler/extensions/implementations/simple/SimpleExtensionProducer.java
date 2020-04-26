package gearth.services.extensionhandler.extensions.implementations.simple;

import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducerObserver;

public class SimpleExtensionProducer implements ExtensionProducer {

    @Override
    public void startProducing(ExtensionProducerObserver observer) {

        // uncomment the next line if you want to see an embedded example extension in G-Earth
        // observer.onExtensionConnect(new ExampleExtension());

    }
}
