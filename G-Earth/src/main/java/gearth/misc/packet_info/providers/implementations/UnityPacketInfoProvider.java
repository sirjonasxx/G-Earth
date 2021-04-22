package gearth.misc.packet_info.providers.implementations;

import gearth.Main;
import gearth.misc.packet_info.PacketInfo;
import gearth.misc.packet_info.providers.PacketInfoProvider;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

public class UnityPacketInfoProvider extends PacketInfoProvider {

    public UnityPacketInfoProvider(String hotelVersion) {
        super(hotelVersion);
    }

    @Override
    protected File getFile() {
        try {
            return new File(new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParentFile(), "messages.json");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected List<PacketInfo> parsePacketInfo(JSONObject jsonObject) {
        return null;
    }
}
