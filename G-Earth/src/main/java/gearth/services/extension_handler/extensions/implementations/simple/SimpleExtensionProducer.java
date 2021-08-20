package gearth.services.extension_handler.extensions.implementations.simple;

import gearth.extensions.InternalExtensionFormBuilder;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.services.internal_extensions.extensionstore.GExtensionStore;
import gearth.services.internal_extensions.extensionstore.GExtensionStoreLauncher;

public class SimpleExtensionProducer implements ExtensionProducer {

    @Override
    public void startProducing(ExtensionProducerObserver observer) {

        // uncomment the next line if you want to see an embedded example extension in G-Earth
//         observer.onExtensionProduced(new ExampleExtension());

        new InternalExtensionFormBuilder<GExtensionStoreLauncher, GExtensionStore>()
                .launch(new GExtensionStoreLauncher(), observer);

    }
}
