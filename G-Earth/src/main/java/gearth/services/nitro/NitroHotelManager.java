package gearth.services.nitro;

import gearth.services.nitro.hotels.HabboCity;

import java.util.ArrayList;
import java.util.List;

public class NitroHotelManager {

    private final List<NitroHotel> hotels;

    public NitroHotelManager() {
        this.hotels = new ArrayList<>();
        this.hotels.add(new HabboCity());
    }

    public boolean hasWebsocket(final String websocketUrl) {
        for (NitroHotel hotel : hotels) {
            if (hotel.hasWebsocket(websocketUrl)) {
                return true;
            }
        }

        return false;
    }

    public NitroHotel getByWebsocket(final String websocketUrl) {
        for (NitroHotel hotel : hotels) {
            if (hotel.hasWebsocket(websocketUrl)) {
                return hotel;
            }
        }

        throw new IllegalArgumentException("No hotel found for websocket url: " + websocketUrl);
    }
}
