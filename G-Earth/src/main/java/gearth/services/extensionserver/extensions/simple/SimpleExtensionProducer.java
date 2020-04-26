package gearth.services.extensionserver.extensions.simple;

import gearth.services.extensionserver.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extensionserver.extensions.extensionproducers.ExtensionProducerObserver;

public class SimpleExtensionProducer implements ExtensionProducer {

    @Override
    public void startProducing(ExtensionProducerObserver observer) {

        // uncomment the next line if you want to see an embedded example extension in G-Earth
        // observer.onExtensionConnect(new ExampleExtension());

    }
}
