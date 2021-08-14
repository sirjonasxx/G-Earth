package gearth.services.extension_handler.extensions.implementations.simple;

import gearth.extensions.InternalExtensionFormBuilder;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.services.internal_extensions.blockreplacepackets.BlockAndReplacePackets;
import gearth.services.internal_extensions.blockreplacepackets.BlockAndReplacePacketsLauncher;
import gearth.services.internal_extensions.packetinfoexplorer.PacketInfoExplorer;
import gearth.services.internal_extensions.packetinfoexplorer.PacketInfoExplorerLauncher;

public class SimpleExtensionProducer implements ExtensionProducer {

    @Override
    public void startProducing(ExtensionProducerObserver observer) {

        // uncomment the next line if you want to see an embedded example extension in G-Earth
//         observer.onExtensionProduced(new ExampleExtension());

        new InternalExtensionFormBuilder<BlockAndReplacePacketsLauncher, BlockAndReplacePackets>()
                .launch(new BlockAndReplacePacketsLauncher(), observer);

        new InternalExtensionFormBuilder<PacketInfoExplorerLauncher, PacketInfoExplorer>()
                .launch(new PacketInfoExplorerLauncher(), observer);
    }
}
