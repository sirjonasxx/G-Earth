package gearth.services.packet_info.providers.implementations;

import gearth.protocol.connection.HClient;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.providers.RemotePacketInfoProvider;
import gearth.protocol.HMessage;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SulekPacketInfoProvider extends RemotePacketInfoProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SulekPacketInfoProvider.class);
    public static final String CACHE_PREFIX = "SULEK_API";
    public static final String SULEK_API_URL_GLOBAL = "https://api.sulek.dev/releases/$hotelversion$/messages";
    public static final String SULEK_API_URL_VARIANT = "https://api.sulek.dev/releases/$variant$/$hotelversion$/messages";
    public static final String SULEK_API_URL_RELEASES = "https://api.sulek.dev/releases?variant=$variant$";

    private final HClient client;

    private String latestHotelVersion;

    public SulekPacketInfoProvider(HClient client, String hotelVersion) {
        super(hotelVersion);
        this.client = client;
    }

    @Override
    public String getHotelVersion() {
        if (latestHotelVersion != null) {
            return latestHotelVersion;
        }

        return hotelVersion;
    }

    @Override
    protected String getRemoteUrl() {
        if (client == HClient.SHOCKWAVE) {
            return SULEK_API_URL_VARIANT.replace("$variant$", "shockwave-windows").replace("$hotelversion$", getHotelVersion());
        }

        return SULEK_API_URL_GLOBAL.replace("$hotelversion$", getHotelVersion());
    }

    @Override
    protected File getFile() {
        if (latestHotelVersion == null && client == HClient.SHOCKWAVE) {
            latestHotelVersion = fetchLatestHotelVersion("shockwave-windows");
        }

        return super.getFile();
    }

    @Override
    protected String getCacheName() {
        return String.format("%s-%s-%s", CACHE_PREFIX, client.toString(), getHotelVersion());
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

    private String fetchLatestHotelVersion(final String variant) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            final HttpGet request = new HttpGet(SULEK_API_URL_RELEASES.replace("$variant$", variant));
            final String response = client.execute(request, res -> res.getCode() == 200
                    ? EntityUtils.toString(res.getEntity())
                    : null);

            if (response != null) {
                final JSONArray jsonArray = new JSONArray(response);
                final JSONObject jsonObject = jsonArray.getJSONObject(0);

                return jsonObject.getString("version");
            }
        } catch (Exception e) {
            LOG.error("Failed to fetch latest hotel version", e);
        }

        return null;
    }
}
