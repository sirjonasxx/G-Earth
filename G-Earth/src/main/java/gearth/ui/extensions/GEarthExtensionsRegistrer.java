package gearth.ui.extensions;

import gearth.protocol.HPacket;
import gearth.ui.extensions.authentication.Authenticator;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by Jonas on 21/06/18.
 */
public class GEarthExtensionsRegistrer {

    private ServerSocketChannel serverSocket;
    private Selector selector;

    GEarthExtensionsRegistrer(ExtensionRegisterObserver observer) {

        int port = 9092;
        boolean serverSetup = false;
        while (!serverSetup) {
            serverSetup = createServer(port);
            port++;
        }

        new Thread(() -> {
            try {
                ByteBuffer lenBuf = ByteBuffer.allocateDirect(4);
                while (selector.isOpen()) {
                    selector.select();

                    Iterator keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = (SelectionKey) keys.next();
                        keys.remove();

                        if (!key.isValid()) continue;

                        if (key.isAcceptable())
                            accept(key);
                        else if (key.isReadable()) {
                            SocketChannel channel = (SocketChannel) key.channel();
                            if (!channel.isOpen())
                               continue;

                            if(channel.read(lenBuf) == -1) {
                                channel.close();
                                continue;
                            }

                            ByteBuffer headerAndBody = ByteBuffer.allocateDirect(4 + lenBuf.getInt(0));
                            lenBuf.flip();
                            headerAndBody.putInt(0);// this will be the length

                            channel.read(headerAndBody);

                            HPacket packet = new HPacket(headerAndBody);
                            packet.fixLength();

                            if (packet.headerId() == Extensions.INCOMING_MESSAGES_IDS.EXTENSIONINFO) {
                                GEarthExtension gEarthExtension = new GEarthExtension(
                                        packet,
                                        channel,
                                        observer::onDisconnect
                                );
                                key.attach(gEarthExtension);

                                if (Authenticator.evaluate(gEarthExtension)) {
                                    observer.onConnect(gEarthExtension);
                                } else {
                                    gEarthExtension.closeConnection(); //you shall not pass...
                                }
                            } else if (key.attachment() != null)
                                ((GEarthExtension)key.attachment()).act(packet);
                        }
                    }
                }
            } catch (IOException e) {e.printStackTrace();}
        }).start();
    }

    private void accept(SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        try {
            SocketChannel channel = serverChannel.accept();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);


                ByteBuffer arr = ByteBuffer.wrap((new HPacket(Extensions.OUTGOING_MESSAGES_IDS.INFOREQUEST)).toBytes());
                channel.write(arr);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean createServer(int port) {
        try {
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(port));
            selector = Selector.open();
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public int getPort() {
        try {
            return ((InetSocketAddress) serverSocket.getLocalAddress()).getPort();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public interface ExtensionRegisterObserver {
        void onConnect(GEarthExtension extension);
        void onDisconnect(GEarthExtension extension);
    }
}
