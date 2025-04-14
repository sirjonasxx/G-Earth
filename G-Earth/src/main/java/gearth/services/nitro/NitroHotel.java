package gearth.services.nitro;

import java.util.List;

public abstract class NitroHotel {

    private final String name;
    private final List<String> websocketUrls;
    private final List<NitroAsset> assetWhitelist;

    public NitroHotel(final String name, final List<String> websocketUrls, final List<NitroAsset> assetWhitelist) {
        this.name = name;
        this.websocketUrls = websocketUrls;
        this.assetWhitelist = assetWhitelist;
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

    public void checkAsset(final String host, final String uri, final byte[] data) {
        for (NitroAsset asset : assetWhitelist) {
            if (asset.matches(host, uri)) {
                loadAsset(host, uri, data);
                return;
            }
        }
    }

    /**
     * Retrieve a packet handler for this hotel.
     * @return Return a new instance of a packet handler, or null for the default packet handler.
     */
    public abstract NitroPacketModifier createPacketModifier();

    /**
     * Proxy loaded an asset for this hotel.
     *
     * @param host The host.
     * @param uri The uri path.
     * @param data The data of the asset.
     */
    protected abstract void loadAsset(final String host, final String uri, final byte[] data);
}
