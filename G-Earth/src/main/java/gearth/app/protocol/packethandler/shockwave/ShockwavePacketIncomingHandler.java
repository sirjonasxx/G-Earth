package gearth.app.protocol.packethandler.shockwave;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.app.protocol.TrafficListener;
import gearth.app.protocol.packethandler.ByteArrayUtils;
import gearth.app.protocol.packethandler.shockwave.buffers.ShockwaveInBuffer;
import gearth.protocol.packethandler.shockwave.packets.ShockPacket;
import gearth.app.services.extension_handler.ExtensionHandler;

import java.io.OutputStream;

public class ShockwavePacketIncomingHandler extends ShockwavePacketHandler {

    private static final byte[] PACKET_END = new byte[] {0x01};
    private static final int ID_SECRET_KEY = 1;

    public ShockwavePacketIncomingHandler(OutputStream outputStream, ExtensionHandler extensionHandler, Observable<TrafficListener>[] trafficObservables, ShockwavePacketHandler outgoingHandler) {
        super(HMessage.Direction.TOCLIENT, new ShockwaveInBuffer(), outputStream, extensionHandler, trafficObservables);

        trafficObservables[0].addListener(new TrafficListener() {
            @Override
            public void onCapture(HMessage message) {
                if (!(message.getPacket() instanceof ShockPacket)) {
                    return;
                }

                final ShockPacket packet = (ShockPacket) message.getPacket();

                if (!packet.canSendToClient()) {
                    return;
                }

                if (packet.headerId() == ID_SECRET_KEY) {
                    logger.info("Received SECRET_KEY from server, enabling encryption / decryption.");
                    trafficObservables[0].removeListener(this);
                    outgoingHandler.setEncryptedStream();
                }
            }
        });
    }

    @Override
    public boolean sendToStream(byte[] packet) {
        return super.sendToStream(ByteArrayUtils.combineByteArrays(packet, PACKET_END));
    }
}
