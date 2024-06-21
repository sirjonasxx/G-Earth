package gearth.protocol.packethandler.shockwave;

import gearth.encoding.Base64Encoding;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.TrafficListener;
import gearth.protocol.packethandler.ByteArrayUtils;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveOutBuffer;
import gearth.services.extension_handler.ExtensionHandler;

import java.io.OutputStream;

public class ShockwavePacketOutgoingHandler extends ShockwavePacketHandler {
    public ShockwavePacketOutgoingHandler(OutputStream outputStream, ExtensionHandler extensionHandler, Observable<TrafficListener>[] trafficObservables) {
        super(HMessage.Direction.TOSERVER, new ShockwaveOutBuffer(), outputStream, extensionHandler, trafficObservables);
    }

    @Override
    public boolean sendToStream(byte[] packet) {
        byte[] bufferLen = Base64Encoding.encode(packet.length, 3);
        byte[] buffer = ByteArrayUtils.combineByteArrays(bufferLen, packet);

        return super.sendToStream(buffer);
    }
}
