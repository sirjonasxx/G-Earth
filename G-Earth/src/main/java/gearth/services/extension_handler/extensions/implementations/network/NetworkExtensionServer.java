package gearth.services.extension_handler.extensions.implementations.network;

import gearth.protocol.HPacket;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducer;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducerFactory;
import gearth.services.extension_handler.extensions.extensionproducers.ExtensionProducerObserver;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionCodec.PacketStructure;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.List;
import java.util.function.BiConsumer;

import static gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionMessage.*;

/**
 * Represents an {@link ExtensionProducer} that implements a server to which
 * remotely-ran extensions can connect.
 *
 * @see ExtensionProducerFactory#getAll() for instance creation.
 *
 * @author Dorving, Jonas
 */
public final class NetworkExtensionServer implements ExtensionProducer {

    private final static Logger LOGGER = LoggerFactory.getLogger(NetworkExtensionServer.class);

    /**
     * Initial port server tries to listen at, if {@link ServerSocket} creation fails,
     * it tries next port.
     */
    private static final int PORT_ONSET = 9092;

    /**
     * The port at which the server is listening.
     */
    private int port = -1;

    @Override
    public void startProducing(ExtensionProducerObserver observer) {

        final ServerBootstrap bootstrap = new ServerBootstrap()
                .option(ChannelOption.TCP_NODELAY, true)
                .childHandler(new Initializer(observer))
                .group(new NioEventLoopGroup());

        port = PORT_ONSET;
        while (!available(port))
            port++;
        LOGGER.debug("Found open port {}, attempting to bind...", port);

        final ChannelFuture channelFuture = bootstrap.bind(port).awaitUninterruptibly();
        if (!channelFuture.isSuccess())
            LOGGER.error("Failed to bind to port {}", port);
        else
            LOGGER.debug("Successfully bound to port {}", port);
    }

    /**
     * The port that the server is bound to.
     *
     * @return the port number to which the server is bound or -1 if the socket is not bound (yet).
     */
    public int getPort() {
        return port;
    }

    /**
     * Checks to see if a specific port is available.
     *
     * Taken from <a href="http://svn.apache.org/viewvc/camel/trunk/components/camel-test/src/main/java/org/apache/camel/test/AvailablePortFinder.java?view=markup#l130">http://svn.apache.org/viewvc/camel/trunk/components/camel-test/src/main/java/org/apache/camel/test/AvailablePortFinder.java?view=markup#l130</a>
     *
     * @param port the port to check for availability
     */
    private static boolean available(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }

    static class Initializer extends ChannelInitializer<SocketChannel> {

        private final ExtensionProducerObserver observer;

        public Initializer(ExtensionProducerObserver observer) {
            this.observer = observer;
        }

        @Override
        protected void initChannel(SocketChannel ch) {
            ch.pipeline()
                    .addLast("decoder", new Decoder())
                    .addLast("encoder", new Encoder())
                    .addLast("handler", new Handler(observer));
            ch.writeAndFlush(new Outgoing.InfoRequest());
        }
    }

    static class Decoder extends ByteToMessageDecoder {

        private final static int HEADER_LENGTH = Integer.BYTES;
        private final static Logger LOGGER = LoggerFactory.getLogger(Decoder.class);

        private volatile Stage stage = Stage.LENGTH;
        private volatile int payloadLength = 0;

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            switch (stage) {
                case LENGTH:
                    payloadLength = in.readInt();
                    stage = Stage.PAYLOAD;
                break;
                case PAYLOAD:

                    if (in.readableBytes() < payloadLength)
                        return;

                    try {

                        final byte[] data = new byte[HEADER_LENGTH + payloadLength];
                        in.readBytes(data, HEADER_LENGTH, payloadLength);

                        final HPacket hPacket = new HPacket(data);
                        hPacket.fixLength();

                        final PacketStructure incomingPacketStructure = NetworkExtensionCodec.getIncomingStructure(hPacket.headerId());
                        if (incomingPacketStructure != null) {
                            final NetworkExtensionMessage message = incomingPacketStructure.getReader().apply(hPacket);
                            out.add(message);
                        } else {
                            LOGGER.error("Did not find decoder for packet {}", hPacket);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to decode message", e);
                    } finally {
                        payloadLength = 0;
                        stage = Stage.LENGTH;
                    }
                break;
            }
        }

        enum Stage {
            LENGTH,
            PAYLOAD
        }
    }

    static class Encoder extends MessageToByteEncoder<Outgoing> {

        @Override
        protected void encode(ChannelHandlerContext ctx, Outgoing msg, ByteBuf out) {
            final PacketStructure structure = NetworkExtensionCodec.getOutgoingStructure(msg);
            if (structure == null){
                LOGGER.error("Structure for Outgoing message not defined (msg={})", msg);
                return;
            }
            try {
                final HPacket hPacket = new HPacket(structure.getHeaderId());
                final BiConsumer<Outgoing, HPacket> writer = (BiConsumer<Outgoing, HPacket>) structure.getWriter();
                writer.accept(msg, hPacket);
                out.writeBytes(hPacket.toBytes());
            } catch (Exception e) {
                LOGGER.error("Failed to encode Outgoing message as a HPacket (msg={})", msg, e);
            }
        }
    }

    static class Handler extends ChannelInboundHandlerAdapter {

        private static final AttributeKey<NetworkExtensionClient> CLIENT = AttributeKey.valueOf("client");

        private final ExtensionProducerObserver observer;

        public Handler(ExtensionProducerObserver observer) {
            this.observer = observer;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            LOGGER.trace("Channel registered (channel={})", ctx.channel());
            super.handlerAdded(ctx);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            LOGGER.trace("Channel unregistered (channel={})", ctx.channel());
            super.channelUnregistered(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            final Channel channel = ctx.channel();
            final Attribute<NetworkExtensionClient> clientAttribute = ctx.attr(CLIENT);
            NetworkExtensionClient client = clientAttribute.get();
            if (msg instanceof Incoming.ExtensionInfo) {
                if (client != null)
                    LOGGER.warn("Overriding pre-existing CLIENT for channel (client={}, channel={})", client, channel);
                client = new NetworkExtensionClient(channel, (Incoming.ExtensionInfo) msg);
                if (NetworkExtensionAuthenticator.evaluate(client)) {
                    LOGGER.info("Successfully authenticated client {}", client);
                    clientAttribute.set(client);
                    observer.onExtensionProduced(client);
                } else {
                    LOGGER.warn("Failed to authenticate client {}, closing connection", client);
                    client.close();
                }
            }
            else if (client == null)
                LOGGER.error("Client was null, could not handle incoming message {}, expected {} first", msg, Incoming.ExtensionInfo.class);
            else if (msg instanceof Incoming)
                client.handleIncomingMessage((Incoming) msg);
            else
                LOGGER.error("Read invalid message type (message={})", msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOGGER.error("Channel exception caught (channel={}), closing channel", ctx.channel(), cause);
            ctx.channel().close();
        }
    }
}
