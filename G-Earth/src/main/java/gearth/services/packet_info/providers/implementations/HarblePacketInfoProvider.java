package gearth.services.packet_info.providers.implementations;

import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.providers.RemotePacketInfoProvider;
import gearth.protocol.HMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HarblePacketInfoProvider extends RemotePacketInfoProvider {

    public static final String CACHE_PREFIX = "HARBLE_API-";
    public static final String HARBLE_API_URL = "https://api.harble.net/messages/$hotelversion$.json";

    public HarblePacketInfoProvider(String hotelVersion) {
        super(hotelVersion);
    }

    @Override
    protected String getRemoteUrl() {
        return HARBLE_API_URL.replace("$hotelversion$", hotelVersion);
    }

    @Override
    protected String getCacheName() {
        return CACHE_PREFIX + hotelVersion;
    }

    private PacketInfo jsonToPacketInfo(JSONObject object, HMessage.Direction destination) {
        String name;
        String hash;
        String structure;
        try {
            name = object.getString("Name")
                    .replaceAll("Composer$", "");
        }
        catch (Exception e) { name = null; }
        try { hash = object.getString("Hash"); }
        catch (Exception e) { hash = null; }
        try { structure = object.getString("Structure");
        } catch (Exception e) { structure = null; }
        structure = (structure == null || structure.equals("")) ? null : structure;

        int headerId;
        try {headerId = object.getInt("Id"); }
        catch (Exception e) { headerId = Integer.parseInt(object.getString("Id")); }

        return new PacketInfo(destination, headerId, hash, name, structure, "Harble");
    }

    @Override
    protected List<PacketInfo> parsePacketInfo(JSONObject jsonObject) {
        List<PacketInfo> packetInfos = new ArrayList<>();

        try {
            JSONArray incoming = jsonObject.getJSONArray("Incoming");
            JSONArray outgoing = jsonObject.getJSONArray("Outgoing");

            if (incoming != null && outgoing != null) {
                for (int i = 0; i < incoming.length(); i++) {
                    JSONObject jsonInfo = incoming.getJSONObject(i);
                    PacketInfo packetInfo = jsonToPacketInfo(jsonInfo, HMessage.Direction.TOCLIENT);
                    packetInfos.add(packetInfo);
                }
                for (int i = 0; i < outgoing.length(); i++) {
                    JSONObject jsonInfo = outgoing.getJSONObject(i);
                    PacketInfo packetInfo = jsonToPacketInfo(jsonInfo, HMessage.Direction.TOSERVER);
                    packetInfos.add(packetInfo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return packetInfos;
    }
}
