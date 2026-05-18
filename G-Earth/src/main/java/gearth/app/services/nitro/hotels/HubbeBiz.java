package gearth.app.services.nitro.hotels;

import gearth.app.services.nitro.NitroHotel;
import gearth.app.services.nitro.NitroPacketModifier;

import java.util.Collections;

public class HubbeBiz extends NitroHotel {
    public HubbeBiz() {
        super("hubbe.biz",
                Collections.singletonList("wss://socket.hubbe.biz/"),
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
