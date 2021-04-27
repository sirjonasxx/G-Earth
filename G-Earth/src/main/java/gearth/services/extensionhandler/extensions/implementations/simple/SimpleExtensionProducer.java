package gearth.services.extensionhandler.extensions.implementations.simple;

import gearth.extensions.InternalExtensionFormBuilder;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.services.internal_extensions.blockreplacepackets.BlockAndReplacePackets;
import gearth.services.internal_extensions.packetinfoexplorer.PacketInfoExplorer;

public class SimpleExtensionProducer implements ExtensionProducer {

    @Override
    public void startProducing(ExtensionProducerObserver observer) {

        // uncomment the next line if you want to see an embedded example extension in G-Earth
//         observer.onExtensionProduced(new ExampleExtension());

        new InternalExtensionFormBuilder<BlockAndReplacePackets>()
                .launch(BlockAndReplacePackets.class, observer);

        new InternalExtensionFormBuilder<PacketInfoExplorer>()
                .launch(PacketInfoExplorer.class, observer);
    }
}
