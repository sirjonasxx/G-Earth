package gearth.services.packet_info.providers.implementations;

import gearth.Main;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.providers.PacketInfoProvider;
import gearth.protocol.HMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GEarthUnityPacketInfoProvider extends PacketInfoProvider {

    public GEarthUnityPacketInfoProvider(String hotelVersion) {
        super(hotelVersion);
    }

    @Override
    protected File getFile() {
        try {
            return new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParentFile(), "messages.json");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    private PacketInfo jsonToPacketInfo(JSONObject object, HMessage.Direction destination) {
        String name = object.getString("Name");
        int headerId = object.getInt("Id");
        return new PacketInfo(destination, headerId, null, name, null, "G-Earth");
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
