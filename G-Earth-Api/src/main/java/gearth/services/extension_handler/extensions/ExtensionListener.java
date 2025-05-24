package gearth.services.extension_handler.extensions;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;

public abstract class ExtensionListener {

    // override whatever you need
    protected void manipulatedPacket(HMessage hMessage) {}
    protected void flagsRequest() {}
    protected void sendMessage(HMessage.Direction direction, HPacket packet) {}
    public void log(String text) {}
    protected void hasClosed() {}

    protected void packetToStringRequest(HPacket packet) {}
    @Deprecated
    protected void stringToPacketRequest(String string) {
        // Kept for backwards compatibility with old extensions
        stringToPacketRequest(string, HPacketFormat.EVA_WIRE);
    }
    protected void stringToPacketRequest(String string, HPacketFormat format) {}

}
