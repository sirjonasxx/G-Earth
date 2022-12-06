package gearth.protocol.connection.proxy.nitro.http;

public interface NitroHttpProxyServerCallback {

    /**
     * Specify a replacement for the given websocket url.
     *
     * @param configUrl The url at which the websocket url was found.
     * @param websocketUrl The hotel websocket url.
     * @return Return null to not replace anything, otherwise specify an alternative websocket url.
     */
    String replaceWebsocketServer(String configUrl, String websocketUrl);

    /**
     * Sets the parsed cookies for the origin WebSocket connection.
     */
    void setOriginCookies(String cookieHeaderValue);

}
