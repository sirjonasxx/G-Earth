package gearth.app.extensions.extra.tools;

import gearth.extensions.ExtensionBase;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.PacketInfoManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GAsync {
    private PacketInfoManager packetInfoManager;
    private final ArrayList<AwaitingPacket> awaitingPackets = new ArrayList<>();

    public GAsync(ExtensionBase ext) {
        this.packetInfoManager = ext.getPacketInfoManager();
        ext.onConnect((host, port, hotelversion, clientIdentifier, clientType) ->
                this.packetInfoManager = ext.getPacketInfoManager()
        );

        ext.intercept(HMessage.Direction.TOSERVER, this::onMessageToServer);
        ext.intercept(HMessage.Direction.TOCLIENT, this::onMessageToClient);
    }

    private void onMessageToClient(HMessage hMessage) {
        if (packetInfoManager != null) {
            List<PacketInfo> packetInfoList = packetInfoManager.getAllPacketInfoFromHeaderId(HMessage.Direction.TOCLIENT, hMessage.getPacket().headerId());
            for (PacketInfo packetInfo : packetInfoList) {
                synchronized (awaitingPackets) {
                    awaitingPackets.stream()
                            .filter(p -> p.getDirection().equals(HMessage.Direction.TOCLIENT))
                            .filter(p -> p.getHeaderName().equals(packetInfo.getName()))
                            .filter(p -> p.test(hMessage))
                            .forEach(p -> p.setPacket(hMessage.getPacket()));
                }
            }
        }
    }

    private void onMessageToServer(HMessage hMessage) {
        if (packetInfoManager != null) {
            List<PacketInfo> packetInfoList = packetInfoManager.getAllPacketInfoFromHeaderId(HMessage.Direction.TOSERVER, hMessage.getPacket().headerId());
            for (PacketInfo packetInfo : packetInfoList) {
                synchronized (awaitingPackets) {
                    awaitingPackets.stream()
                            .filter(p -> p.getDirection().equals(HMessage.Direction.TOSERVER))
                            .filter(p -> p.getHeaderName().equals(packetInfo.getName()))
                            .filter(p -> p.test(hMessage))
                            .forEach(p -> p.setPacket(hMessage.getPacket()));
                }
            }
        }
    }

    public HPacket awaitPacket(AwaitingPacket ...packets) {
        synchronized (awaitingPackets) {
            awaitingPackets.addAll(Arrays.asList(packets));
        }

        while (true) {
            for (AwaitingPacket packet : packets) {
                if (packet.isReady()) {
                    synchronized (awaitingPackets) {
                        awaitingPackets.removeAll(Arrays.asList(packets));
                    }
                    return packet.getPacket();
                }
            }
            sleep(1);
        }
    }

    public HPacket[] awaitMultiplePackets(AwaitingPacket ...packets) {
        synchronized (awaitingPackets) {
            awaitingPackets.addAll(Arrays.asList(packets));
        }

        while (true) {
            if (Arrays.stream(packets).allMatch(AwaitingPacket::isReady)) {
                synchronized (awaitingPackets) {
                    awaitingPackets.removeAll(Arrays.asList(packets));
                }
                return Arrays.stream(packets)
                        .map(AwaitingPacket::getPacket)
                        .toArray(HPacket[]::new);
            }
            sleep(1);
        }
    }

    public void clear() {
        synchronized (awaitingPackets) {
            awaitingPackets.clear();
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
