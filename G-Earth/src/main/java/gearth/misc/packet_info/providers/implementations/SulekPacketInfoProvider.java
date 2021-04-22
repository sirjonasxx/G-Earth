package gearth.misc.packet_info.providers.implementations;

import gearth.misc.packet_info.PacketInfo;
import gearth.misc.packet_info.providers.RemotePacketInfoProvider;
import org.json.JSONObject;

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

    @Override
    protected List<PacketInfo> parsePacketInfo(JSONObject jsonObject) {
        return null;
    }
}
