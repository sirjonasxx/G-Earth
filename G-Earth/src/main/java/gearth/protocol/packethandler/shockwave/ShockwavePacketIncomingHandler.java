package gearth.protocol.packethandler.shockwave;

import gearth.protocol.HMessage;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveInBuffer;
import gearth.services.extension_handler.ExtensionHandler;

import java.io.OutputStream;

public class ShockwavePacketIncomingHandler extends ShockwavePacketHandler {
    public ShockwavePacketIncomingHandler(OutputStream outputStream, ExtensionHandler extensionHandler, Object[] trafficObservables) {
        super(HMessage.Direction.TOCLIENT, new ShockwaveInBuffer(), outputStream, extensionHandler, trafficObservables);
    }
}
