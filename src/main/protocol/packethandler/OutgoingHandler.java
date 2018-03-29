package main.protocol.packethandler;

import main.protocol.HMessage;
import main.protocol.HPacket;
import main.protocol.memory.Rc4Obtainer;

import java.io.IOException;
import java.io.OutputStream;

public class OutgoingHandler extends Handler {

    private final static int encryptOffset = 3; //all packets with index < 3 aren't encrypted

    public OutgoingHandler(OutputStream outputStream) {
        super(outputStream);
    }

    private void dataStreamCheck(byte[] buffer)	{
        if (!isDataStream) {
            HPacket hpacket = new HPacket(buffer);
            isDataStream = (hpacket.getBytesLength() > 6 && hpacket.headerId() == 4000 && hpacket.headerId() == 4000);
            if (isDataStream) {
                Rc4Obtainer.initialize();
                Rc4Obtainer.rc4Obtainer.setOutgoingHandler(this);
            }
        }
    }

    @Override
    public void act(byte[] buffer, Object[] listeners) throws IOException {
        dataStreamCheck(buffer);
        super.act(buffer, listeners);
    }

    @Override
    public void sendToStream(byte[] buffer) {

    }

    @Override
    public void flush() throws IOException {

        if (currentIndex < encryptOffset) {
            HPacket[] hpackets = payloadBuffer.receive();
            for (HPacket hpacket : hpackets){
                HMessage hMessage = new HMessage(hpacket, HMessage.Side.TOSERVER, currentIndex);
                notifyListeners(hMessage);
                if (!hMessage.isBlocked())	{
                    out.write(hMessage.getPacket().toBytes());
                }
                currentIndex ++;
            }
        }

        if (currentIndex >= encryptOffset) {
            if (payloadBuffer.peak().length > 0) {
                HPacket packet = new HPacket(payloadBuffer.forceClear());
                HMessage hMessage = new HMessage(packet, HMessage.Side.TOSERVER, currentIndex);

                notifyListeners(hMessage);
                out.write(packet.toBytes());
                currentIndex++;
            }

        }
    }
}
