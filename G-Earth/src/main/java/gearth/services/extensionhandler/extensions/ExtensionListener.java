package gearth.services.extensionhandler.extensions;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

public abstract class ExtensionListener {

    // override whatever you need
    protected void manipulatedPacket(HMessage hMessage) {}
    protected void flagsRequest() {}
    protected void sendMessage(HMessage.Direction direction, HPacket packet) {}
    protected void log(String text) {}
    protected void hasClosed() {}

    protected void packetToStringRequest(HPacket packet) {}
    protected void stringToPacketRequest(String string) {}

}
