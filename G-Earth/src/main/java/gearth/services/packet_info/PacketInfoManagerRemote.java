package gearth.services.packet_info;

import gearth.protocol.connection.HClient;
import gearth.services.packet_info.providers.RemotePacketInfoProvider;
import gearth.services.packet_info.providers.implementations.GEarthUnityPacketInfoProvider;
import gearth.services.packet_info.providers.implementations.HarblePacketInfoProvider;
import gearth.services.packet_info.providers.implementations.SulekPacketInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

public class PacketInfoManagerRemote {

    private static final Logger LOG = LoggerFactory.getLogger(PacketInfoManagerRemote.class);

    public static PacketInfoManager fromHotelVersion(String hotelversion, HClient clientType) {
        final AtomicReference<String> version = new AtomicReference<>(hotelversion);
        final List<PacketInfo> result = new ArrayList<>();

        if (clientType == HClient.UNITY) {
            result.addAll(new GEarthUnityPacketInfoProvider(hotelversion).provide());
        } else if (clientType == HClient.FLASH || clientType == HClient.NITRO || clientType == HClient.SHOCKWAVE) {
            try {
                List<RemotePacketInfoProvider> providers = new ArrayList<>();
                if (clientType != HClient.SHOCKWAVE) {
                    providers.add(new HarblePacketInfoProvider(hotelversion));
                }
                providers.add(new SulekPacketInfoProvider(clientType, hotelversion));

                Semaphore blockUntilComplete = new Semaphore(providers.size());
                blockUntilComplete.acquire(providers.size());

                List<PacketInfo> synchronizedResult = Collections.synchronizedList(result);
                for (RemotePacketInfoProvider provider : providers) {
                    new Thread(() -> {
                        final List<PacketInfo> packets = provider.provide();
                        if (!packets.isEmpty()) {
                            synchronizedResult.addAll(packets);
                            version.set(provider.getHotelVersion());
                        }
                        blockUntilComplete.release();
                    }).start();
                }

                blockUntilComplete.acquire(providers.size());

            } catch (InterruptedException e) {
                LOG.error("Error while waiting for packet info providers to finish", e);
            }
        }

        return new PacketInfoManager(version.get(), result);
    }

}
