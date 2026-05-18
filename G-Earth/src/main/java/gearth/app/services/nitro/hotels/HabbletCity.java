package gearth.app.services.nitro.hotels;

import gearth.app.services.nitro.NitroHotel;
import gearth.app.services.nitro.NitroPacketModifier;

import java.util.Collections;

public class HabbletCity extends NitroHotel {

    public HabbletCity() {
        super("habblet.city",
                Collections.singletonList("wss://proxy.habblet.city/"),
                Collections.emptyList());
    }

    @Override
    public NitroPacketModifier createPacketModifier() {
        return new LeetNL.LeetNLPacketModifier();
    }

    @Override
    protected void loadAsset(String host, String uri, byte[] data) {
    }
}
