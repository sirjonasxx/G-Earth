package main.protocol.memory;

import main.protocol.HConnection;
import main.protocol.HPacket;
import main.protocol.crypto.RC4;
import main.protocol.memory.habboclient.HabboClient;
import main.protocol.memory.habboclient.HabboClientFactory;
import main.protocol.memory.habboclient.linux.LinuxHabboClient;
import main.protocol.packethandler.IncomingHandler;
import main.protocol.packethandler.OutgoingHandler;
import main.protocol.packethandler.PayloadBuffer;

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
            if (!hashappened1 && handler.getCurrentIndex() == 3) {
                hashappened1 = true;
                onSendFirstEncryptedMessage();
            }
        });
    }


    public void setIncomingHandler(IncomingHandler handler) {
        incomingHandler = handler;
    }


    private void onSendFirstEncryptedMessage() {
        outgoingHandler.block();
        incomingHandler.block();
        new Thread(() -> {
            if (DEBUG) System.out.println("[+] send encrypted");


            List<byte[]> results = client.getRC4possibilities();
            outerloop:
            for (byte[] possible : results) {

                byte[] encBuffer = new byte[outgoingHandler.getEncryptedBuffer().size()];
                for (int i = 0; i < encBuffer.length; i++) {
                    encBuffer[i] = outgoingHandler.getEncryptedBuffer().get(i);
                }

                for (int i = 0; i < 256; i++) {
//                    System.out.println(i);
                    for (int j = 0; j < 256; j++) {
                        byte[] keycpy = Arrays.copyOf(possible, possible.length);
                        RC4 rc4Tryout = new RC4(keycpy, i, j);

                        rc4Tryout.undoRc4(encBuffer);
                        if (rc4Tryout.couldBeFresh()) {
                            byte[] encDataCopy = Arrays.copyOf(encBuffer, encBuffer.length);
                            RC4 rc4TryCopy = rc4Tryout.deepCopy();

                            try {
                                PayloadBuffer payloadBuffer = new PayloadBuffer();
                                HPacket[] checker = payloadBuffer.pushAndReceive(rc4TryCopy.rc4(encDataCopy));

                                if (payloadBuffer.peak().length == 0) {
                                    outgoingHandler.setRc4(rc4Tryout);
                                    incomingHandler.setRc4(rc4Tryout);
                                    break outerloop;
                                }

                            }
                            catch (Exception e) {

                            }

                        }

                    }
                }
                

            }
            

            incomingHandler.unblock();
            outgoingHandler.unblock();
        }).start();
    }
}
