package gearth.services.packet_info.providers.implementations;

import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.providers.RemotePacketInfoProvider;
import gearth.protocol.HMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SulekPacketInfoProvider extends RemotePacketInfoProvider {

    public static final String CACHE_PREFIX = "SULEK_API-";
    public static final String SULEK_API_URL = "https://api.sulek.dev/releases/$hotelversion$/messages";

    public SulekPacketInfoProvider(String hotelVersion) {
        super(hotelVersion);
    }

    @Override
    protected String getRemoteUrl() {
        return SULEK_API_URL.replace("$hotelversion$", hotelVersion);
    }

    @Override
    protected String getCacheName() {
        return CACHE_PREFIX + hotelVersion;
    }

    private PacketInfo jsonToPacketInfo(JSONObject object, HMessage.Direction destination) {
        int headerId = object.getInt("id");
        String name = object.getString("name")
                .replaceAll("(((Message)?Composer)|((Message)?Event))$", "");

        return new PacketInfo(destination, headerId, null, name, null, "Sulek");
    }

    @Override
    protected List<PacketInfo> parsePacketInfo(JSONObject jsonObject) {
        List<PacketInfo> packetInfos = new ArrayList<>();

        try {
            JSONArray incoming = jsonObject.getJSONObject("messages").getJSONArray("incoming");
            JSONArray outgoing = jsonObject.getJSONObject("messages").getJSONArray("outgoing");

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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return packetInfos;
    }
}
