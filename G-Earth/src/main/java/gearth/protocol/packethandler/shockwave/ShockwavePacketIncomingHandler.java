package gearth.protocol.packethandler.shockwave;

import gearth.protocol.HMessage;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveInBuffer;
import gearth.services.extension_handler.ExtensionHandler;

import java.io.IOException;
import java.io.OutputStream;

public class ShockwavePacketIncomingHandler extends ShockwavePacketHandler {
    public ShockwavePacketIncomingHandler(OutputStream outputStream, ExtensionHandler extensionHandler, Object[] trafficObservables) {
        super(HMessage.Direction.TOCLIENT, new ShockwaveInBuffer(), outputStream, extensionHandler, trafficObservables);
    }

    @Override
    public boolean sendToStream(byte[] buffer) {
        synchronized (sendLock) {
            try {
                outputStream.write(buffer);
                outputStream.write(new byte[] {0x01});
                return true;
            } catch (IOException e) {
                logger.error("Error while sending packet to stream.", e);
                return false;
            }
        }
    }
}
