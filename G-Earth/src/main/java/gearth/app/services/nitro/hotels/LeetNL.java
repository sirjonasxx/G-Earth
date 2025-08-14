package gearth.app.services.nitro.hotels;

import gearth.app.services.nitro.NitroHotel;
import gearth.app.services.nitro.NitroPacketModifier;

import java.util.Collections;

public class LeetNL extends NitroHotel {

    public LeetNL() {
        super("leet.city",
                Collections.singletonList("wss://proxy.leet.city/"),
                Collections.emptyList());
    }

    @Override
    public NitroPacketModifier createPacketModifier() {
        return null;
    }

    @Override
    protected void loadAsset(String host, String uri, byte[] data) {
    }
}
