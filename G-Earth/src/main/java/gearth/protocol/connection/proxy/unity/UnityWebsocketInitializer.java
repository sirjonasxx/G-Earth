package gearth.protocol.connection.proxy.unity;

import gearth.protocol.connection.proxy.http.HttpProxyCertificateFactory;
import gearth.protocol.connection.proxy.nitro.NitroConstants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;

public class UnityWebsocketInitializer extends ChannelInitializer<SocketChannel> {

    private final UnityCommunicatorConfig config;

    public UnityWebsocketInitializer(final UnityCommunicatorConfig config) {
        this.config = config;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        final HttpProxyCertificateFactory factory = new HttpProxyCertificateFactory();

        if (!factory.loadOrCreate()) {
            throw new IllegalStateException("Failed to load or create CA certificate");
        }

        final ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast("ssl", factory.createSslHandler("localhost"));
        pipeline.addLast("httpServerCodec", new HttpServerCodec());
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("websocketCompression", new WebSocketServerCompressionHandler());
        pipeline.addLast("websocketAggregator", new WebSocketFrameAggregator(NitroConstants.WEBSOCKET_BUFFER_SIZE));
        pipeline.addLast("httpHandler", new UnityWebsocketHandler.HttpServer(config));
    }
}
