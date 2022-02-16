package gearth.misc;

import gearth.protocol.HPacket;

import java.util.HashMap;

public class HostInfo {

    private final String packetlogger;
    private final String version;
    private final HashMap<String, String> attributes;

    public HostInfo(String packetlogger, String version, HashMap<String, String> attributes) {
        this.packetlogger = packetlogger;
        this.version = version;
        this.attributes = attributes;
    }

    public static HostInfo fromPacket(HPacket packet) {
        String packetlogger = packet.readString();
        String version = packet.readString();
        int attributeCount = packet.readInteger();
        HashMap<String, String> attributes = new HashMap<>();
        for (int i = 0; i < attributeCount; i++) {
            String key = packet.readString();
            String value = packet.readString();
            attributes.put(key, value);
        }
        return new HostInfo(packetlogger, version, attributes);
    }

    public void appendToPacket(HPacket packet) {
        packet.appendString(packetlogger);
        packet.appendString(version);
        packet.appendInt(attributes.size());
        attributes.keySet().forEach(k -> {
            packet.appendString(k);
            packet.appendString(attributes.get(k));
        });
    }

    public String getPacketlogger() {
        return packetlogger;
    }

    public String getVersion() {
        return version;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }
}
