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

    private Boolean isEncryptedStream = null;


    @Override
    public void act(byte[] buffer) throws IOException {
        if (isDataStream)	{
            if (DEBUG) {
                printForDebugging(buffer);
            }


            if (isEncryptedStream == null || !isEncryptedStream) {
                payloadBuffer.push(buffer);
            }
            else {
                payloadBuffer.push(servercipher.rc4(buffer));
            }


            notifyBufferListeners(buffer.length);

            if (!isTempBlocked) {
                flush();
            }
        }
        else  {
            out.write(buffer);
        }
    }

    @Override
    public void sendToStream(byte[] buffer) {
        synchronized (lock) {
            try {
                out.write(
                        (isEncryptedStream == null || !isEncryptedStream)
                                ? buffer
                                : clientcipher.rc4(buffer)
                );
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
                if (isDataStream) {
                    notifyListeners(hMessage);
                }

                if (!hMessage.isBlocked())	{
                    out.write(
                            (isEncryptedStream == null || !isEncryptedStream)
                            ? hMessage.getPacket().toBytes()
                            : clientcipher.rc4(hMessage.getPacket().toBytes())
                    );
                }

                if (isDataStream && isEncryptedStream == null && hpacket.length() == 261) {
                    isEncryptedStream = hpacket.readBoolean(264);
                }
                currentIndex++;
            }
        }

    }

    @Override
    protected void printForDebugging(byte[] bytes) {
        System.out.println("-- DEBUG INCOMING -- " + new HPacket(bytes).toString() + " -- DEBUG --");
    }
}
