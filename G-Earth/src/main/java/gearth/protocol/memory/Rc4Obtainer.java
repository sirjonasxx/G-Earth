package gearth.protocol.memory;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.crypto.RC4;
import gearth.protocol.memory.habboclient.HabboClient;
import gearth.protocol.memory.habboclient.HabboClientFactory;
import gearth.protocol.packethandler.Handler;
import gearth.protocol.packethandler.IncomingHandler;
import gearth.protocol.packethandler.OutgoingHandler;
import gearth.protocol.packethandler.PayloadBuffer;

import java.util.Arrays;
import java.util.List;

public class Rc4Obtainer {

    public static final boolean DEBUG = false;

    HabboClient client = null;
    OutgoingHandler outgoingHandler = null;
    IncomingHandler incomingHandler = null;

    public Rc4Obtainer(HConnection hConnection) {
        client = HabboClientFactory.get(hConnection);
    }

    private boolean hashappened1 = false;
    public void setOutgoingHandler(OutgoingHandler handler) {
        outgoingHandler = handler;
        handler.addBufferListener((int addedbytes) -> {
            if (!hashappened1 && handler.isEncryptedStream()) {
                hashappened1 = true;
                onSendFirstEncryptedMessage(outgoingHandler);
            }
        });
    }

    private boolean hashappened2 = false;
    public void setIncomingHandler(IncomingHandler handler) {
        incomingHandler = handler;
        handler.addBufferListener((int addedbytes) -> {
            if (!hashappened2 && handler.isEncryptedStream()) {
                hashappened2 = true;
                onSendFirstEncryptedMessage(incomingHandler);
            }
        });
    }


    private void onSendFirstEncryptedMessage(Handler handler) {
        outgoingHandler.block();
        incomingHandler.block();

        new Thread(() -> {

            if (DEBUG) System.out.println("[+] send encrypted");

            boolean worked = false;
            int i = 0;
            while (!worked && i < 4) {
                worked = (i % 2 == 0) ?
                        onSendFirstEncryptedMessage(handler, client.getRC4cached()) :
                        onSendFirstEncryptedMessage(handler, client.getRC4possibilities());
                i++;
            }

            if (!worked) {
                System.err.println("COULD NOT FIND RC4 TABLE");
            }

            incomingHandler.unblock();
            outgoingHandler.unblock();
        }).start();
    }

    private boolean onSendFirstEncryptedMessage(Handler handler, List<byte[]> potentialRC4tables) {
        for (byte[] possible : potentialRC4tables) {

            byte[] encBuffer = new byte[handler.getEncryptedBuffer().size()];
            for (int i = 0; i < encBuffer.length; i++) {
                encBuffer[i] = handler.getEncryptedBuffer().get(i);
            }

            for (int i = 0; i < 256; i++) {
                for (int j = 0; j < 256; j++) {
                    byte[] keycpy = Arrays.copyOf(possible, possible.length);
                    RC4 rc4Tryout = new RC4(keycpy, i, j);

                    if (handler.getMessageSide() == HMessage.Side.TOSERVER) rc4Tryout.undoRc4(encBuffer);
                    if (rc4Tryout.couldBeFresh()) {
                        byte[] encDataCopy = Arrays.copyOf(encBuffer, encBuffer.length);
                        RC4 rc4TryCopy = rc4Tryout.deepCopy();

                        try {
                            PayloadBuffer payloadBuffer = new PayloadBuffer();
                            byte[] decoded = rc4TryCopy.rc4(encDataCopy);
                            HPacket[] checker = payloadBuffer.pushAndReceive(decoded);

                            if (payloadBuffer.peak().length == 0) {
                                handler.setRc4(rc4Tryout);
                                return true;
                            }

                        } catch (Exception e) {
//                                e.printStackTrace();
                        }

                    }

                }
            }
        }
        return false;
    }
}
