package gearth.protocol.packethandler.shockwave;

import gearth.protocol.HMessage;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveOutBuffer;
import gearth.services.extension_handler.ExtensionHandler;

import java.io.OutputStream;

public class ShockwavePacketOutgoingHandler extends ShockwavePacketHandler {
    public ShockwavePacketOutgoingHandler(OutputStream outputStream, ExtensionHandler extensionHandler, Object[] trafficObservables) {
        super(HMessage.Direction.TOSERVER, new ShockwaveOutBuffer(), outputStream, extensionHandler, trafficObservables);
    }
}
