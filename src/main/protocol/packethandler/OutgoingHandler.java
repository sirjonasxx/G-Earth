package main.protocol.packethandler;

import main.protocol.HMessage;
import main.protocol.HPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class OutgoingHandler extends Handler {

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
            if (!isEncryptedStream && (new HPacket(buffer).length() < 2 || new HPacket(buffer).length() > 1000)) {
                isEncryptedStream = true;
            }

            continuedAct(buffer);
        }
        else  {
            out.write(buffer);
        }
    }


    @Override
    protected void printForDebugging(byte[] bytes) {
        System.out.println("-- DEBUG OUTGOING -- " + new HPacket(bytes).toString() + " -- DEBUG --");
    }
}
