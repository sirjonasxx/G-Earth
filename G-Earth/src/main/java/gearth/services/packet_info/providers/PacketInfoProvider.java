package gearth.services.packet_info.providers;

import gearth.services.packet_info.PacketInfo;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public abstract class PacketInfoProvider {

    protected final String hotelVersion;

    public PacketInfoProvider(String hotelVersion) {
        this.hotelVersion = hotelVersion;
    }

    protected abstract File getFile();

    public List<PacketInfo> provide() {
        File file = getFile();
        if (file == null || !file.exists() || file.isDirectory()) return new ArrayList<>();

        try {
            String contents = String.join("\n", Files.readAllLines(file.toPath()));
            JSONObject object = new JSONObject(contents);
            return parsePacketInfo(object);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    protected abstract List<PacketInfo> parsePacketInfo(JSONObject jsonObject);

}
