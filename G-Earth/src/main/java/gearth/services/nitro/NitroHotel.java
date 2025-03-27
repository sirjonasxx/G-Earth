package gearth.services.nitro;

import java.util.List;

public abstract class NitroHotel {

    private final String name;
    private final List<String> websocketUrls;

    public NitroHotel(final String name, final List<String> websocketUrls) {
        this.name = name;
        this.websocketUrls = websocketUrls;
    }

    public String getName() {
        return name;
    }

    public boolean hasWebsocket(final String websocketUrl) {
        return websocketUrls.contains(websocketUrl);
    }

    public List<String> getWebsocketUrls() {
        return websocketUrls;
    }

    /**
     * Retrieve a packet handler for this hotel.
     * @return Return a new instance of a packet handler, or null for the default packet handler.
     */
    public abstract NitroPacketModifier createPacketModifier();

}
