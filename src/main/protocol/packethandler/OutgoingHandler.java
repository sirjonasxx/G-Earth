package main.protocol.packethandler;

import main.protocol.HMessage;
import main.protocol.HPacket;
import main.protocol.crypto.RC4;
import main.protocol.memory.Rc4Obtainer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class OutgoingHandler extends Handler {

    private final Object lock = new Object();


    private final static int encryptOffset = 3; //all packets with index < 3 aren't encrypted
    private RC4 clientcipher = null;
    private RC4 servercipher = null;
    private List<Byte> tempEncryptedBuffer = new ArrayList<>();

    public OutgoingHandler(OutputStream outputStream, Object[] listeners) {
        super(outputStream, listeners);
    }

    private void dataStreamCheck(byte[] buffer)	{
        if (!isDataStream) {
            HPacket hpacket = new HPacket(buffer);
            isDataStream = (hpacket.getBytesLength() > 6 && hpacket.length() < 100);
        }
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        dataStreamCheck(buffer);
        if (isDataStream)	{

            if (currentIndex < encryptOffset) {
                payloadBuffer.push(buffer);
            }
            else if (clientcipher == null) {
                for (int i = 0; i < buffer.length; i++) {
                    tempEncryptedBuffer.add(buffer[i]);
                }
            }
            else {
                payloadBuffer.push(clientcipher.rc4(buffer));
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
                out.write(servercipher.rc4(buffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void setRc4(RC4 rc4) {
        this.clientcipher = rc4;
        this.servercipher = rc4.deepCopy();

        byte[] encrbuffer = new byte[tempEncryptedBuffer.size()];
        for (int i = 0; i < tempEncryptedBuffer.size(); i++) {
            encrbuffer[i] = tempEncryptedBuffer.get(i);
        }

        try {
            act(encrbuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tempEncryptedBuffer = null;
    }
    public List<Byte> getEncryptedBuffer() {
        return tempEncryptedBuffer;
    }

    @Override
    public void flush() throws IOException {
        synchronized (lock) {
            HPacket[] hpackets = payloadBuffer.receive();
            for (HPacket hpacket : hpackets){
                HMessage hMessage = new HMessage(hpacket, HMessage.Side.TOSERVER, currentIndex);
                if (isDataStream) notifyListeners(hMessage);
                if (!hMessage.isBlocked())	{
                    out.write(
                            currentIndex < encryptOffset ? hMessage.getPacket().toBytes() :
                                    servercipher.rc4(hMessage.getPacket().toBytes())
                    );
                }
                currentIndex ++;
            }
        }

    }
}
