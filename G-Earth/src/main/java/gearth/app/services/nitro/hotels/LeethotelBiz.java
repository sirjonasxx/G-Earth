package gearth.app.services.nitro.hotels;

import gearth.app.services.nitro.NitroHotel;
import gearth.app.services.nitro.NitroPacketModifier;

import java.util.Collections;

public class LeethotelBiz extends NitroHotel {

    public LeethotelBiz() {
        super("leethotel.biz",
                Collections.singletonList("wss://proxy.leethotel.biz/"),
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
