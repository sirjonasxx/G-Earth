package gearth.app.services.nitro.hotels;

import gearth.app.services.nitro.NitroHotel;
import gearth.app.services.nitro.NitroPacketModifier;

import java.util.Collections;
import java.util.List;

public class Leet extends NitroHotel {

    public Leet() {
        super("leet",
                List.of("wss://proxy.leet.city/*",
                        "wss://proxy.leethotel.biz/*",
                        "wss://game.habblet.city/*"),
                Collections.emptyList());
    }

    @Override
    public NitroPacketModifier createPacketModifier(String websocketUrl) {
        return null;
    }

    @Override
    protected void loadAsset(String host, String uri, byte[] data) {
    }
}
