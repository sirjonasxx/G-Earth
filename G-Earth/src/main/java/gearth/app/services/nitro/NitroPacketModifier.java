package gearth.app.services.nitro;

import gearth.app.protocol.connection.proxy.nitro.NitroPacketEvent;

public interface NitroPacketModifier {

    void clientToGearth(final NitroPacketEvent e) throws Exception;

    void gearthToClient(final NitroPacketEvent e) throws Exception;

    void serverToGearth(final NitroPacketEvent e) throws Exception;

    void gearthToServer(final NitroPacketEvent e) throws Exception;

}
