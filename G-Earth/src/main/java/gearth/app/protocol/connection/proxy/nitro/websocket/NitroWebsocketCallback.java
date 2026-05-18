package gearth.app.protocol.connection.proxy.nitro.websocket;

import io.netty.channel.Channel;

public interface NitroWebsocketCallback {

    void onConnected(String websocketUrl, Channel client, Channel server);

    void onHandshakeComplete();

    void onClose();

    void onClientMessage(final byte[] buffer);

    void onServerMessage(final byte[] buffer);

}
