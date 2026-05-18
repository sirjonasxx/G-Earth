package gearth.app.protocol.connection.proxy.nitro.websocket;

import gearth.app.protocol.connection.proxy.nitro.NitroPacketEvent;

public interface NitroNettyModifier {

    void modify(NitroPacketEvent event) throws Exception;

}
