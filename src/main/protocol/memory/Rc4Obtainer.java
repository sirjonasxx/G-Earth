package main.protocol.memory;

import javafx.application.Platform;
import main.protocol.HPacket;
import main.protocol.packethandler.BufferListener;
import main.protocol.packethandler.IncomingHandler;
import main.protocol.packethandler.OutgoingHandler;
import main.protocol.memory.FlashClient;

import java.util.List;

public class Rc4Obtainer {

    public static Rc4Obtainer rc4Obtainer = null;
    public static void initialize() {
        rc4Obtainer = new Rc4Obtainer();
        rc4Obtainer.client = FlashClient.create();
    }

    FlashClient client = null;
    OutgoingHandler outgoingHandler = null;
    IncomingHandler incomingHandler = null;

    private Rc4Obtainer() {

    }

    public void setOutgoingHandler(OutgoingHandler handler) {
        outgoingHandler = handler;
        handler.addBufferListener(() -> {
            if (handler.getCurrentIndex() >= 3) hasChangedFromLastCheck = true;
            if (!hashappened1 && handler.getCurrentIndex() == 3) {
                hashappened1 = true;
                onSendFirstEncryptedMessage();
            }
        });
    }

    public void setIncomingHandler(IncomingHandler handler) {
        incomingHandler = handler;
        handler.addBufferListener(() -> {
            if (!hashappened2 && handler.getCurrentIndex() == 1) {
                hashappened2 = true;
                onReceivePubKey();
            }
        });
    }

    private List<FlashClient.MemorySnippet> fullmemorybeforekey = null;
    private boolean hasChangedFromLastCheck = false;

    private boolean hashappened2 = false;
    private void onReceivePubKey() {
        incomingHandler.block();
        new Thread(() -> {

            System.out.println("[+] receive pubkey");
            client.pauseProcess();

            fullmemorybeforekey = client.createMemorySnippetList();
            client.fetchMemory(fullmemorybeforekey);

            System.out.println("[-] receive pubkey");

            client.resumeProcess();
            incomingHandler.unblock();
        }).start();
    }


    private boolean hashappened1 = false;
    private void onSendFirstEncryptedMessage() {
        incomingHandler.block();
        outgoingHandler.block();
        new Thread(() -> {

            System.out.println("[+] send encrypted");
            client.pauseProcess();
            client.updateMapLocationsSnippetList(fullmemorybeforekey);
            client.resumeProcess();

            List<FlashClient.MemorySnippet> diff = searchForPossibleRC4Tables(fullmemorybeforekey);
            System.out.println("size: " + getTotalBytesLengthOfDiff(diff));
            for (int i = 0; i < 20; i++) {
//                if (i % 2 == 1) {
//                    incomingHandler.sendToStream(new HPacket(3631).toBytes());
//                }
                sleep(200);
                boolean rem = hasChangedFromLastCheck;
                diff = searchForPossibleRC4Tables(diff);
                System.out.println("size: " + getTotalBytesLengthOfDiff(diff) + " and was changed: " + rem);
            }


            System.out.println("[-] send encrypted");
            outgoingHandler.unblock();
            incomingHandler.unblock();
        }).start();


    }

    private List<FlashClient.MemorySnippet> searchForPossibleRC4Tables(List<FlashClient.MemorySnippet> snippets) {
        List<FlashClient.MemorySnippet> result;
        client.pauseProcess();
        if (hasChangedFromLastCheck) {
            result = client.differentiate(snippets, true, 255);
            hasChangedFromLastCheck = false;
        }
        else {
            result = client.differentiate(snippets, false, 4);
        }
        client.resumeProcess();

        return result;
    }

    private long getTotalBytesLengthOfDiff(List<FlashClient.MemorySnippet> snippets) {
        long tot = 0;
        for (FlashClient.MemorySnippet snippet : snippets) {
            tot += (snippet.getData().length);
        }
        return tot;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    //        System.out.println("[+] receive pubkey");
//        incomingHandler.block();
//        client.pauseProcess();
//        fullmemorybeforekey = client.createMemorySnippetList();
//        client.fetchMemory(fullmemorybeforekey);
//        System.out.println("[-] receive pubkey");
//        incomingHandler.unblock();
//        client.resumeProcess();

    //        client.pauseProcess();
//
//        client.refreshMemoryMaps();
//        List<String> result = client.findSharedKey2();
//        System.out.println("result size: "+ result);
//
//        client.resumeProcess();

//        System.out.println("[+] send encrypted");
//        client.pauseProcess();
//        client.updateMapLocationsSnippetList(fullmemorybeforekey);
//
//        List<FlashClient.MemorySnippet> diff = client.differentiate(fullmemorybeforekey, true, 54);
//
//        List<String> results = client.findSharedKey(diff);
//        System.out.println("results: " +results.size());
//        for (String s : results) {
//            System.out.println(s);
//        }
//        System.out.println("[-] send encrypted");
//        client.resumeProcess();

//     payloadBuffer.push(buffer);
//    buffer = new byte[]{};
//    tempBlockIncoming = true;
//    client = FlashClient.create();
//     client.pauseProcess();
//    fullmemoryb4publickey = client.createMemorySnippetList();
//      client.fetchMemory(fullmemoryb4publickey);
//      client.resumeProcess();



//     if (!doneFlash) {
//        tempBlockEncrypted = true;
//        FlashClient client = IncomingHandler.client;
//        List<FlashClient.MemorySnippet> mem = IncomingHandler.fullmemoryb4publickey;
//        client.pauseProcess();
//        client.updateMapLocationsSnippetList(mem);
//        List<FlashClient.MemorySnippet> diff = client.differentiate(mem, true, 54);
//        IncomingHandler.fullmemoryb4publickey = null;
//        List<String> results = client.findSharedKey(diff);
//        System.out.println("results: " +results.size());
//        for (String s : results) {
//            System.out.println(s);
//        }
//        client.resumeProcess();
//        tempBlockEncrypted = false;
//        IncomingHandler.tempBlockIncoming = false;
//        doneFlash = true;
//    }
}
