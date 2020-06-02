package gearth.services.extensionhandler.extensions.implementations.network;

import gearth.protocol.HPacket;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extensionhandler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.services.extensionhandler.extensions.implementations.network.authentication.Authenticator;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jonas on 21/06/18.
 */
public class NetworkExtensionsProducer implements ExtensionProducer {

    private ServerSocket serverSocket;

    @Override
    public void startProducing(ExtensionProducerObserver observer) {
//        serverSocket = new ServerSocket(0);
        int port = 9092;
        boolean serverSetup = false;
        while (!serverSetup) {
            serverSetup = createServer(port);
            port++;
        }


        new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket extensionSocket = serverSocket.accept();
                    extensionSocket.setTcpNoDelay(true);

                    new Thread(() -> {
                        try {
                            synchronized (extensionSocket) {
                                extensionSocket.getOutputStream().write((new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.INFOREQUEST)).toBytes());
                            }

                            InputStream inputStream = extensionSocket.getInputStream();
                            DataInputStream dIn = new DataInputStream(inputStream);

                            while (!extensionSocket.isClosed()) {

                                int length = dIn.readInt();
                                byte[] headerandbody = new byte[length + 4];

                                int amountRead = 0;
                                while (amountRead < length) {
                                    amountRead += dIn.read(headerandbody, 4 + amountRead, Math.min(dIn.available(), length - amountRead));
                                }

                                HPacket packet = new HPacket(headerandbody);
                                packet.fixLength();

                                if (packet.headerId() == NetworkExtensionInfo.INCOMING_MESSAGES_IDS.EXTENSIONINFO) {
                                    NetworkExtension gEarthExtension = new NetworkExtension(
                                            packet,
                                            extensionSocket
                                    );

                                    if (Authenticator.evaluate(gEarthExtension)) {
                                        observer.onExtensionProduced(gEarthExtension);
                                    }
                                    else {
                                        gEarthExtension.close(); //you shall not pass...
                                    }

                                    break;
                                }
                            }

                        } catch (IOException ignored) {}
                    }).start();
                }
            } catch (IOException e) {e.printStackTrace();}
        }).start();
    }

    private boolean createServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }
}
