package gearth.protocol.packethandler.flash;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.TrafficListener;
import gearth.protocol.packethandler.EncryptedPacketHandler;
import gearth.protocol.packethandler.PayloadBuffer;
import gearth.services.extension_handler.ExtensionHandler;

import java.io.IOException;
import java.io.OutputStream;

public abstract class FlashPacketHandler extends EncryptedPacketHandler {

    private final OutputStream out;
    private final PayloadBuffer payloadBuffer;
    private volatile boolean isDataStream;

    FlashPacketHandler(HMessage.Direction direction, OutputStream outputStream, Observable<TrafficListener>[] trafficObservables, ExtensionHandler extensionHandler) {
        super(extensionHandler, trafficObservables, direction);
        this.out = outputStream;
        this.payloadBuffer = new PayloadBuffer();
        this.isDataStream = false;
    }

    public boolean isDataStream() {
        return isDataStream;
    }

    public void setAsDataStream() {
        isDataStream = true;
    }

    public void act(byte[] buffer) throws IOException {
        if (!isDataStream) {
            synchronized (sendLock) {
                out.write(buffer);
            }
            return;
        }

        super.act(buffer);

        if (!isBlocked()) {
            flush();
        }
    }

    @Override
    protected void writeOut(byte[] buffer) throws IOException {
        synchronized (sendLock) {
            out.write(buffer);
        }
    }

    @Override
    protected void writeBuffer(byte[] buffer) {
        payloadBuffer.push(buffer);
    }

    public boolean sendToStream(byte[] buffer) {
        return sendToStream(buffer, isEncryptedStream());
    }

    private boolean sendToStream(byte[] buffer, boolean isEncrypted) {
        synchronized (sendLock) {
            try {
                out.write(isEncrypted ? encrypt(buffer) : buffer);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public void flush() throws IOException {
        synchronized (flushLock) {
            HPacket[] hpackets = payloadBuffer.receive();

            for (HPacket hpacket : hpackets){
                HMessage hMessage = new HMessage(hpacket, getDirection(), currentIndex);
                boolean isencrypted = isEncryptedStream();

                if (isDataStream) {
                    awaitListeners(hMessage, hMessage1 -> sendToStream(hMessage1.getPacket().toBytes(), isencrypted));
                }
                else {
                    sendToStream(hMessage.getPacket().toBytes(), isencrypted);
                }

                currentIndex++;
            }
        }
    }
}
