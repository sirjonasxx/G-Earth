package gearth.app.services.extension_handler.extensions.implementations.simple;

import gearth.app.extensions.InternalExtensionFormLauncher;
import gearth.app.services.extension_handler.extensions.extensionproducers.ExtensionProducer;
import gearth.app.services.extension_handler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.app.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.app.services.internal_extensions.extensionstore.GExtensionStoreCreator;

public class SimpleExtensionProducer implements ExtensionProducer {

    @Override
    public void startProducing(ExtensionProducerObserver observer) {

        // uncomment the next line if you want to see an embedded example extension in G-Earth
//         observer.onExtensionProduced(new ExampleExtension());

        new InternalExtensionFormLauncher<GExtensionStoreCreator, GExtensionStore>()
                .launch(new GExtensionStoreCreator(), observer);

    }
}
