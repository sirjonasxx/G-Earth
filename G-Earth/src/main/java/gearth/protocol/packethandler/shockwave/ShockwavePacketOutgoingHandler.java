package gearth.protocol.packethandler.shockwave;

import gearth.encoding.Base64Encoding;
import gearth.protocol.HMessage;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveOutBuffer;
import gearth.services.extension_handler.ExtensionHandler;

import java.io.IOException;
import java.io.OutputStream;

public class ShockwavePacketOutgoingHandler extends ShockwavePacketHandler {
    public ShockwavePacketOutgoingHandler(OutputStream outputStream, ExtensionHandler extensionHandler, Object[] trafficObservables) {
        super(HMessage.Direction.TOSERVER, new ShockwaveOutBuffer(), outputStream, extensionHandler, trafficObservables);
    }

    @Override
    public boolean sendToStream(byte[] buffer) {
        synchronized (sendLock) {
            try {
                byte[] bufferLen = Base64Encoding.encode(buffer.length, 3);

                outputStream.write(bufferLen);
                outputStream.write(buffer);
                return true;
            } catch (IOException e) {
                logger.error("Error while sending packet to stream.", e);
                return false;
            }
        }
    }
}
