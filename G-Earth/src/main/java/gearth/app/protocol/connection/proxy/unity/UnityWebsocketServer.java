package gearth.app.protocol.connection.proxy.unity;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class UnityWebsocketServer {

    private static final Logger LOG = LoggerFactory.getLogger(UnityWebsocketServer.class);

    private final UnityCommunicatorConfig config;

    private ChannelFuture channelFuture;

    public UnityWebsocketServer(final UnityCommunicatorConfig config) {
        this.config = config;
    }

    public boolean start() {
        final ServerBootstrap b = new ServerBootstrap();

        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()))
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new UnityWebsocketInitializer(config));

        try {
            this.channelFuture = b.bind(0).sync();
            return true;
        } catch (InterruptedException e) {
            LOG.error("Failed to bind unity websocket server", e);
            return false;
        }
    }

    public int getPort() {
        if (channelFuture == null) {
            return -1;
        }

        return ((InetSocketAddress) channelFuture.channel().localAddress()).getPort();
    }

    public void stop() throws InterruptedException {
        if (channelFuture != null) {
            channelFuture.channel().close().sync();
            channelFuture = null;
        }
    }
}
