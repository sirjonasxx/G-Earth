package gearth.services.nitro;

import gearth.services.nitro.hotels.HabboCity;
import gearth.services.nitro.hotels.HabboSK;
import gearth.services.nitro.hotels.Hartico;
import gearth.services.nitro.hotels.LeetNL;

import java.util.ArrayList;
import java.util.List;

public class NitroHotelManager {

    private final List<NitroHotel> hotels;

    public NitroHotelManager() {
        this.hotels = new ArrayList<>();
        this.hotels.add(new HabboCity());
        this.hotels.add(new Hartico());
        this.hotels.add(new LeetNL());
        this.hotels.add(new HabboSK());
    }

    public void checkAsset(final String host, final String uri, final byte[] data) {
        for (NitroHotel hotel : hotels) {
            hotel.checkAsset(host, uri, data);
        }
    }

    public boolean hasWebsocket(String websocketUrl) {
        websocketUrl = normalizeWebsocketUrl(websocketUrl);

        for (NitroHotel hotel : hotels) {
            if (hotel.hasWebsocket(websocketUrl)) {
                return true;
            }
        }

        return false;
    }

    public NitroHotel getByWebsocket(String websocketUrl) {
        websocketUrl = normalizeWebsocketUrl(websocketUrl);

        for (NitroHotel hotel : hotels) {
            if (hotel.hasWebsocket(websocketUrl)) {
                return hotel;
            }
        }

        throw new IllegalArgumentException("No hotel found for websocket url: " + websocketUrl);
    }

    private static String normalizeWebsocketUrl(String websocketUrl) {
        if (websocketUrl.contains("?")) {
            websocketUrl = websocketUrl.substring(0, websocketUrl.indexOf("?"));
        }

        return websocketUrl;
    }
}
