package gearth.misc.packet_info.providers.implementations;

import gearth.misc.Cacher;
import gearth.misc.packet_info.PacketInfo;
import gearth.misc.packet_info.providers.PacketInfoProvider;
import gearth.misc.packet_info.providers.RemotePacketInfoProvider;
import org.json.JSONObject;

import java.io.File;
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

    @Override
    protected List<PacketInfo> parsePacketInfo(JSONObject jsonObject) {
        return null;
    }
}
