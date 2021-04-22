package gearth.misc.packet_info;

import gearth.misc.Cacher;
import gearth.misc.harble_api.HarbleAPIFetcher;
import gearth.misc.packet_info.providers.RemotePacketInfoProvider;
import gearth.misc.packet_info.providers.implementations.HarblePacketInfoProvider;
import gearth.misc.packet_info.providers.implementations.SulekPacketInfoProvider;
import gearth.misc.packet_info.providers.implementations.UnityPacketInfoProvider;
import gearth.protocol.HMessage;
import gearth.protocol.connection.HClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Semaphore;

public class PacketInfoManager {

    private Map<Integer, List<PacketInfo>> headerIdToMessage_incoming = new HashMap<>();
    private Map<Integer, List<PacketInfo>> headerIdToMessage_outgoing = new HashMap<>();

    private Map<String, List<PacketInfo>> hashToMessage_incoming = new HashMap<>();
    private Map<String, List<PacketInfo>> hashToMessage_outgoing = new HashMap<>();

    private Map<String, List<PacketInfo>> nameToMessage_incoming = new HashMap<>();
    private Map<String, List<PacketInfo>> nameToMessage_outgoing = new HashMap<>();

    public PacketInfoManager(List<PacketInfo> packetInfoList) {
        for (PacketInfo packetInfo : packetInfoList) {
            addMessage(packetInfo);
        }
    }

    private void addMessage(PacketInfo packetInfo) {
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

    public PacketInfo getPacketInfoFromHeaderId(HMessage.Direction direction, int headerId) {
        Map<Integer, List<PacketInfo>> headerIdToMessage =
                (direction == HMessage.Direction.TOSERVER
                        ? headerIdToMessage_outgoing
                        : headerIdToMessage_incoming);

        if (headerIdToMessage.get(headerId) == null) return null;
        return headerIdToMessage.get(headerId).get(0);
    }

    public PacketInfo getHarbleMessagesFromHash(HMessage.Direction direction, String hash) {
        Map<String, List<PacketInfo>> hashToMessage =
                (direction == HMessage.Direction.TOSERVER
                        ? hashToMessage_outgoing
                        : hashToMessage_incoming);

        if (hashToMessage.get(hash) == null) return null;
        return hashToMessage.get(hash).get(0);
    }

    public PacketInfo getHarbleMessageFromName(HMessage.Direction direction, String name) {
        Map<String, List<PacketInfo>> nameToMessage =
                (direction == HMessage.Direction.TOSERVER
                        ? nameToMessage_outgoing
                        : nameToMessage_incoming);

        if (nameToMessage.get(name) == null) return null;
        return nameToMessage.get(name).get(0);
    }


    public List<PacketInfo> getAllPacketInfoFromHeaderId(HMessage.Direction direction, int headerId) {
        Map<Integer, List<PacketInfo>> headerIdToMessage =
                (direction == HMessage.Direction.TOSERVER
                        ? headerIdToMessage_outgoing
                        : headerIdToMessage_incoming);

        return headerIdToMessage.get(headerId) == null ? new ArrayList<>() : headerIdToMessage.get(headerId);
    }

    public List<PacketInfo> getAllHarbleMessagesFromHash(HMessage.Direction direction, String hash) {
        Map<String, List<PacketInfo>> hashToMessage =
                (direction == HMessage.Direction.TOSERVER
                        ? hashToMessage_outgoing
                        : hashToMessage_incoming);

        return hashToMessage.get(hash) == null ? new ArrayList<>() : hashToMessage.get(hash);
    }

    public List<PacketInfo> getAllHarbleMessageFromName(HMessage.Direction direction, String name) {
        Map<String, List<PacketInfo>> nameToMessage =
                (direction == HMessage.Direction.TOSERVER
                        ? nameToMessage_outgoing
                        : nameToMessage_incoming);

        return nameToMessage.get(name) == null ? new ArrayList<>() : nameToMessage.get(name);
    }


    public static PacketInfoManager fromHotelVersion(String hotelversion, HClient clientType) {
        List<PacketInfo> result = new ArrayList<>();

        if (clientType == HClient.UNITY) {
            result.addAll(new UnityPacketInfoProvider(hotelversion).provide());
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
}
