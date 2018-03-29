package main.protocol.packethandler;

import main.protocol.HMessage;
import main.protocol.HPacket;
import main.protocol.memory.Rc4Obtainer;

import java.io.IOException;
import java.io.OutputStream;

public class IncomingHandler extends Handler {

    public IncomingHandler(OutputStream outputStream) {
        super(outputStream);
    }

    @Override
    public void setAsDataStream() {
        super.setAsDataStream();
        Rc4Obtainer.rc4Obtainer.setIncomingHandler(this);
    }

    @Override
    public void sendToStream(byte[] buffer) {
        try {
            out.write(buffer);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush() throws IOException {
        HPacket[] hpackets = payloadBuffer.receive();

        for (HPacket hpacket : hpackets){
            HMessage hMessage = new HMessage(hpacket, HMessage.Side.TOCLIENT, currentIndex);
            notifyListeners(hMessage);

            if (!hMessage.isBlocked())	{
                out.write(hMessage.getPacket().toBytes());
            }
            currentIndex++;
        }
    }
}
