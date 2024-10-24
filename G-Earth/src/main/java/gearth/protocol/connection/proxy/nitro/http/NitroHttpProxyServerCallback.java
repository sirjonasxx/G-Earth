package gearth.protocol.connection.proxy.nitro.http;

public interface NitroHttpProxyServerCallback {

    /**
     * Specify a replacement for the given websocket url.
     *
     * @param websocketUrl The hotel websocket url.
     * @return Return null to not replace anything, otherwise specify an alternative websocket url.
     */
    String replaceWebsocketServer(String websocketUrl);

    /**
     * Sets the parsed cookies for the origin WebSocket connection.
     */
    void setOriginCookies(String cookieHeaderValue);

}
