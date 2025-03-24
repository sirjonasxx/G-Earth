package gearth.protocol.connection.proxy.nitro.websocket;

import io.netty.channel.Channel;

public interface NitroWebsocketCallback {

    void onConnected(Channel client, Channel server);

    void onHandshakeComplete();

    void onClose();

    void onClientMessage(byte[] buffer);

    void onServerMessage(byte[] buffer);

}
