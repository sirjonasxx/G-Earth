package gearth.app.services.nitro;

import gearth.app.services.nitro.hotels.ConfiguredNitroHotel;
import gearth.app.services.nitro.hotels.HabboCity;
import gearth.app.services.nitro.hotels.HabboSK;
import gearth.app.services.nitro.hotels.Hartico;
import gearth.app.services.nitro.hotels.HubbeBiz;
import gearth.app.services.nitro.hotels.HubbeSt;
import gearth.app.services.nitro.hotels.Leet;

import java.util.ArrayList;
import java.util.List;

public class NitroHotelManager {

    private final List<NitroHotel> hotels;

    public NitroHotelManager() {
        this.hotels = new ArrayList<>();
        this.hotels.addAll(ConfiguredNitroHotel.load());
        this.hotels.add(new HabboCity());
        this.hotels.add(new Hartico());
        this.hotels.add(new HabboSK());
        this.hotels.add(new HubbeSt());
        this.hotels.add(new HubbeBiz());
        this.hotels.add(new Leet());
    }

    public void checkAsset(final String host, final String uri, final byte[] data) {
        for (NitroHotel hotel : hotels) {
            hotel.checkAsset(host, uri, data);
        }
    }

    public NitroHotel getByWebsocketOrNull(String websocketUrl) {
        for (NitroHotel hotel : hotels) {
            if (hotel.hasWebsocket(websocketUrl)) {
                return hotel;
            }
        }

        return null;
    }

    public static String normalizeWebsocketUrl(String websocketUrl) {
        if (websocketUrl.contains("?")) {
            websocketUrl = websocketUrl.substring(0, websocketUrl.indexOf("?"));
        }

        return websocketUrl;
    }
}
