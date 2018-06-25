package main.protocol.packethandler;

import main.protocol.HMessage;
import main.protocol.HPacket;
import main.protocol.memory.Rc4Obtainer;

import java.io.IOException;
import java.io.OutputStream;

public class IncomingHandler extends Handler {

    public IncomingHandler(OutputStream outputStream, Object[] listeners) {
        super(outputStream, listeners);
    }

    private final Object lock = new Object();

    @Override
    public void sendToStream(byte[] buffer) {
        synchronized (lock) {
            try {
                out.write(buffer);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            HPacket[] hpackets = payloadBuffer.receive();

            for (HPacket hpacket : hpackets){
                HMessage hMessage = new HMessage(hpacket, HMessage.Side.TOCLIENT, currentIndex);
                if (isDataStream) notifyListeners(hMessage);

                if (!hMessage.isBlocked())	{
                    out.write(hMessage.getPacket().toBytes());
                    out.flush();
                }
                currentIndex++;
            }
        }

    }
}
