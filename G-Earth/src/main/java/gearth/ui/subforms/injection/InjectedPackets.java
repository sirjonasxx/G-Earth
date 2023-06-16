package gearth.ui.subforms.injection;

import gearth.misc.StringifyAble;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.PacketInfoManager;
import gearth.ui.translations.LanguageBundle;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InjectedPackets implements StringifyAble {

    private String packetsAsString;
    private String description;

    public InjectedPackets(String packetsAsString, int amountPackets, PacketInfoManager packetInfoManager, HMessage.Direction direction) {
        String description;
        if (amountPackets > 1) {
            description = String.format("(%s: %d, %s: %d)", LanguageBundle.get("tab.injection.description.packets"), amountPackets, LanguageBundle.get("tab.injection.description.length"), packetsAsString.length());
        }
        else { // assume 1 packet
            HPacket packet = new HPacket(packetsAsString);
            String identifier = null;
            if (!packet.isPacketComplete()) {
                identifier = packet.packetIncompleteIdentifier();
            }
            else {
                Optional<PacketInfo> maybeInfo = packetInfoManager.getAllPacketInfoFromHeaderId(direction, packet.headerId())
                        .stream().filter(packetInfo -> packetInfo.getName() != null).findFirst();
                if (maybeInfo.isPresent()) {
                    PacketInfo packetInfo = maybeInfo.get();
                    identifier = packetInfo.getName();
                }
            }

            if (identifier != null) {
                description = String.format("%s", identifier);
            }
            else {
                description = String.format("(%s: %d, %s: %d)", LanguageBundle.get("tab.injection.description.id"), packet.headerId(), LanguageBundle.get("tab.injection.description.length"), packet.length());
            }
        }

        this.description = description;
        this.packetsAsString = packetsAsString;
    }

    public InjectedPackets(String fromString) {
        constructFromString(fromString);
    }

    public String getPacketsAsString() {
        return packetsAsString;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String stringify() {
        Map<String, String> info = new HashMap<>();
        info.put("packetsAsString", packetsAsString);
        info.put("description", description);

        return new JSONObject(info).toString();
    }

    @Override
    public void constructFromString(String str) {
        JSONObject jsonObject = new JSONObject(str);
        this.packetsAsString = jsonObject.getString("packetsAsString");
        this.description = jsonObject.getString("description");
    }

    @Override
    public String toString() {
        return description;
    }
}
