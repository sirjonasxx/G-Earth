package gearth.protocol.packethandler.shockwave;

import gearth.encoding.Base64Encoding;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.TrafficListener;
import gearth.protocol.crypto.RC4Cipher;
import gearth.protocol.packethandler.ByteArrayUtils;
import gearth.protocol.packethandler.shockwave.buffers.ShockwaveOutBuffer;
import gearth.services.extension_handler.ExtensionHandler;

import java.io.OutputStream;
import java.util.concurrent.ThreadLocalRandom;

public class ShockwavePacketOutgoingHandler extends ShockwavePacketHandler {
    private RC4Cipher headerEncoder;

    public ShockwavePacketOutgoingHandler(OutputStream outputStream, ExtensionHandler extensionHandler, Observable<TrafficListener>[] trafficObservables) {
        super(HMessage.Direction.TOSERVER, new ShockwaveOutBuffer(), outputStream, extensionHandler, trafficObservables);
    }

    @Override
    public boolean sendToStream(byte[] packet) {
        byte[] bufferLen;

        if (isEncryptedStream()) {
            if (headerEncoder == null) {
                throw new IllegalStateException("Expected header encoder to be set for an encrypted stream.");
            }

            // Encrypt packet.
            packet = encrypt(packet);

            // Encrypt header.
            final byte[] newPacketLen = Base64Encoding.encode(packet.length, 3);
            final byte[] header = new byte[4];

            header[0] = (byte) ThreadLocalRandom.current().nextInt(0, 127);
            header[1] = newPacketLen[0];
            header[2] = newPacketLen[1];
            header[3] = newPacketLen[2];

            bufferLen = headerEncoder.cipher(header);
        } else {
            bufferLen = Base64Encoding.encode(packet.length, 3);
        }

        final byte[] buffer = ByteArrayUtils.combineByteArrays(bufferLen, packet);

        return super.sendToStream(buffer);
    }

    @Override
    public void setRc4(RC4Cipher rc4) {
        this.headerEncoder = rc4.deepCopy();
        super.setRc4(rc4);
    }
}
