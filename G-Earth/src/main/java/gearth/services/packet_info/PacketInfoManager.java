package gearth.services.packet_info;

import gearth.services.packet_info.providers.RemotePacketInfoProvider;
import gearth.services.packet_info.providers.implementations.HarblePacketInfoProvider;
import gearth.services.packet_info.providers.implementations.SulekPacketInfoProvider;
import gearth.services.packet_info.providers.implementations.GEarthUnityPacketInfoProvider;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;

import java.util.*;
import java.util.concurrent.Semaphore;

public class PacketInfoManager {

    private Map<Integer, List<PacketInfo>> headerIdToMessage_incoming = new HashMap<>();
    private Map<Integer, List<PacketInfo>> headerIdToMessage_outgoing = new HashMap<>();

    private Map<String, List<PacketInfo>> hashToMessage_incoming = new HashMap<>();
    private Map<String, List<PacketInfo>> hashToMessage_outgoing = new HashMap<>();

    private Map<String, List<PacketInfo>> nameToMessage_incoming = new HashMap<>();
    private Map<String, List<PacketInfo>> nameToMessage_outgoing = new HashMap<>();

    private List<PacketInfo> packetInfoList;

    public PacketInfoManager(List<PacketInfo> packetInfoList) {
        this.packetInfoList = packetInfoList;
        for (PacketInfo packetInfo : packetInfoList) {
            addMessage(packetInfo);
        }
    }

    private void addMessage(PacketInfo packetInfo) {
        if (packetInfo.getHash() == null && packetInfo.getName() == null) return;

        Map<Integer, List<PacketInfo>> headerIdToMessage =
                packetInfo.getDestination() == HMessage.Direction.TOCLIENT
                        ? headerIdToMessage_incoming :
                        headerIdToMessage_outgoing;

        Map<String, List<PacketInfo>> hashToMessage =
                packetInfo.getDestination() == HMessage.Direction.TOCLIENT
                        ? hashToMessage_incoming
                        : hashToMessage_outgoing;

        Map<String, List<PacketInfo>> nameToMessage =
                packetInfo.getDestination() == HMessage.Direction.TOCLIENT
                        ? nameToMessage_incoming
                        : nameToMessage_outgoing;

        headerIdToMessage.computeIfAbsent(packetInfo.getHeaderId(), k -> new ArrayList<>());

        headerIdToMessage.get(packetInfo.getHeaderId()).add(packetInfo);
        if (packetInfo.getHash() != null) {
            hashToMessage.computeIfAbsent(packetInfo.getHash(), k -> new ArrayList<>());
            hashToMessage.get(packetInfo.getHash()).add(packetInfo);
        }
        if (packetInfo.getName() != null) {
            nameToMessage.computeIfAbsent(packetInfo.getName(), k -> new ArrayList<>());
            nameToMessage.get(packetInfo.getName()).add(packetInfo);
        }

    }

    public List<PacketInfo> getAllPacketInfoFromHeaderId(HMessage.Direction direction, int headerId) {
        Map<Integer, List<PacketInfo>> headerIdToMessage =
                (direction == HMessage.Direction.TOSERVER
                        ? headerIdToMessage_outgoing
                        : headerIdToMessage_incoming);

        return headerIdToMessage.get(headerId) == null ? new ArrayList<>() : headerIdToMessage.get(headerId);
    }

    public List<PacketInfo> getAllPacketInfoFromHash(HMessage.Direction direction, String hash) {
        Map<String, List<PacketInfo>> hashToMessage =
                (direction == HMessage.Direction.TOSERVER
                        ? hashToMessage_outgoing
                        : hashToMessage_incoming);

        return hashToMessage.get(hash) == null ? new ArrayList<>() : hashToMessage.get(hash);
    }

    public List<PacketInfo> getAllPacketInfoFromName(HMessage.Direction direction, String name) {
        Map<String, List<PacketInfo>> nameToMessage =
                (direction == HMessage.Direction.TOSERVER
                        ? nameToMessage_outgoing
                        : nameToMessage_incoming);

        return nameToMessage.get(name) == null ? new ArrayList<>() : nameToMessage.get(name);
    }

    public PacketInfo getPacketInfoFromHeaderId(HMessage.Direction direction, int headerId) {
        List<PacketInfo> all = getAllPacketInfoFromHeaderId(direction, headerId);
        return all.size() == 0 ? null : all.get(0);
    }

    public PacketInfo getPacketInfoFromHash(HMessage.Direction direction, String hash) {
        List<PacketInfo> all = getAllPacketInfoFromHash(direction, hash);
        return all.size() == 0 ? null : all.get(0);
    }

    public PacketInfo getPacketInfoFromName(HMessage.Direction direction, String name) {
        List<PacketInfo> all = getAllPacketInfoFromName(direction, name);
        return all.size() == 0 ? null : all.get(0);
    }

    public List<PacketInfo> getPacketInfoList() {
        return packetInfoList;
    }

    public static PacketInfoManager fromHotelVersion(String hotelversion, HClient clientType) {
        List<PacketInfo> result = new ArrayList<>();

        if (clientType == HClient.UNITY) {
            result.addAll(new GEarthUnityPacketInfoProvider(hotelversion).provide());
        }
        else if (clientType == HClient.FLASH) {
            try {
                List<RemotePacketInfoProvider> providers = new ArrayList<>();
                providers.add(new HarblePacketInfoProvider(hotelversion));
                providers.add(new SulekPacketInfoProvider(hotelversion));

                Semaphore blockUntilComplete = new Semaphore(providers.size());
                blockUntilComplete.acquire(providers.size());

                List<PacketInfo> synchronizedResult = Collections.synchronizedList(result);
                for (RemotePacketInfoProvider provider : providers) {
                    new Thread(() -> {
                        synchronizedResult.addAll(provider.provide());
                        blockUntilComplete.release();
                    }).start();
                }

                blockUntilComplete.acquire(providers.size());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        return new PacketInfoManager(result);
    }

    public static PacketInfoManager readFromPacket(HPacket hPacket) {
        List<PacketInfo> packetInfoList = new ArrayList<>();
        int size = hPacket.readInteger();

        for (int i = 0; i < size; i++) {
            int headerId = hPacket.readInteger();
            String hash = hPacket.readString();
            String name = hPacket.readString();
            String structure = hPacket.readString();
            boolean isOutgoing = hPacket.readBoolean();
            String source = hPacket.readString();

            packetInfoList.add(new PacketInfo(
                    isOutgoing ? HMessage.Direction.TOSERVER : HMessage.Direction.TOCLIENT,
                    headerId,
                    hash.equals("NULL") ? null : hash,
                    name.equals("NULL") ? null : name,
                    structure.equals("NULL") ? null : structure,
                    source));
        }

        return new PacketInfoManager(packetInfoList);
    }

    public void appendToPacket(HPacket hPacket) {
        hPacket.appendInt(packetInfoList.size());
        for (PacketInfo packetInfo : packetInfoList) {
            hPacket.appendInt(packetInfo.getHeaderId());
            hPacket.appendString(packetInfo.getHash() == null ? "NULL" : packetInfo.getHash());
            hPacket.appendString(packetInfo.getName() == null ? "NULL" : packetInfo.getName());
            hPacket.appendString(packetInfo.getStructure() == null ? "NULL" : packetInfo.getStructure());
            hPacket.appendBoolean(packetInfo.getDestination() == HMessage.Direction.TOSERVER);
            hPacket.appendString(packetInfo.getSource());
        }
    }


    public static PacketInfoManager EMPTY = new PacketInfoManager(new ArrayList<>());
}
