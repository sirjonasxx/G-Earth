package gearth.services.extension_handler.extensions.implementations.network;

import gearth.protocol.HPacket;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducerFactory;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.services.extension_handler.extensions.implementations.network.authentication.Authenticator;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Represents an {@link ExtensionProducer} that implements a server to which
 * remotely-ran extensions can connect.
 *
 * @see ExtensionProducerFactory#getAll() for instance creation.
 *
 * Created by Jonas on 21/06/18.
 */
public final class NetworkExtensionsProducer implements ExtensionProducer {

    /**
     * Initial port server tries to listen at, if {@link ServerSocket} creation fails,
     * it tries next port.
     */
    private static final int PORT_ONSET = 9092;

    /**
     * Represents the number of bytes per boolean encoded in an incoming packet.
     */
    private static final int BOOLEAN_SIZE = 1;

    /**
     * Represents the maximum number of bytes per string encoded in an incoming packet.
     */
    private static final int MAX_STRING_SIZE = Character.BYTES * 4_000;

    /**
     * Length is encoded as an {@link Integer} and header id as an {@link Short}.
     */
    private static final int PACKET_HEADER_SIZE = Integer.BYTES + Short.BYTES;

    /**
     * Represents the maximum number of bytes in the body of an incoming packet.
     * <p>
     * Used as a form of validation for packets, prevents other Apps that connect
     * with the server from sending unexpected data and inexplicably causing huge byte array allocations.
     * <p>
     * Since the server only accepts {@link NetworkExtensionInfo.INCOMING_MESSAGES_IDS#EXTENSIONINFO} packets,
     * this value is calculated based on that packet.
     */
    private static final int MAX_PACKET_BODY_SIZE = (MAX_STRING_SIZE * 6) + (BOOLEAN_SIZE * 4);

    /**
     * The port at which the {@link #serverSocket} is listening for incoming connections.
     */
    public static int extensionPort = -1;

    private ServerSocket serverSocket;

    @Override
    public void startProducing(ExtensionProducerObserver observer) {

        /*
        Initialise the serverSocket at the argued port.
         */
        int port = PORT_ONSET;
        while (!createServer(port))
            ++port;

        /*
        Start connection listener thread.
         */
        new Thread(() -> {

            try {

                while (!serverSocket.isClosed()) {

                    // accept a new connection
                    final Socket extensionSocket = serverSocket.accept();
                    extensionSocket.setTcpNoDelay(true);

                    /*
                    Start client session handler thread.
                     */
                    new Thread(() -> {

                        try {

                            // write INFOREQUEST packet to client
                            synchronized (extensionSocket) {
                                extensionSocket.getOutputStream().write((new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.INFOREQUEST)).toBytes());
                            }

                            final DataInputStream dIn = new DataInputStream(extensionSocket.getInputStream());

                            // listen to incoming data from client
                            while (!extensionSocket.isClosed()) {

                                final int bodyLength = dIn.readInt() - Short.BYTES;
                                final short headerId = dIn.readShort();

                                if (headerId == NetworkExtensionInfo.INCOMING_MESSAGES_IDS.EXTENSIONINFO) {

                                    if (bodyLength > MAX_PACKET_BODY_SIZE) {
                                        System.err.printf("Incoming packet(h=%d, l=%d) exceeds max packet body size %d.", headerId, bodyLength, MAX_PACKET_BODY_SIZE);
                                        break;
                                    }

                                    final HPacket packet = readPacket(dIn, bodyLength, headerId);

                                    final NetworkExtension gEarthExtension = new NetworkExtension(packet, extensionSocket);

                                    if (Authenticator.evaluate(gEarthExtension))
                                        observer.onExtensionProduced(gEarthExtension);
                                    else
                                        gEarthExtension.close();

                                    break;
                                }
                            }
                        } catch (IOException ignored) {
                        }
                    }).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private boolean createServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            extensionPort = port;
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private HPacket readPacket(DataInputStream dIn, int amountToRead, short id) throws IOException {
        final byte[] headerAndBody = new byte[amountToRead + PACKET_HEADER_SIZE];

        int amountRead = 0;
        while (amountRead < amountToRead)
            amountRead += dIn.read(headerAndBody,  amountRead + PACKET_HEADER_SIZE, Math.min(dIn.available(), amountToRead - amountRead));

        final HPacket packet = new HPacket(headerAndBody);
        packet.fixLength();
        packet.replaceShort(4, id); // add header id

        return packet;
    }

    /**
     * Retrieves the {@link ServerSocket#getLocalPort()} of {@link #serverSocket}.
     *
     * @return the port number to which {@link #serverSocket} is listening or -1 if the socket is not bound yet.
     */
    public int getPort() {
        return serverSocket.getLocalPort();
    }
}