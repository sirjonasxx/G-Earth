package gearth.protocol.connection.proxy.unity;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.connection.proxy.http.WebNettySession;
import gearth.protocol.packethandler.unity.UnityPacketHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class UnityWebsocketHandler {

    public static class HttpServer extends ChannelInboundHandlerAdapter {

        private final UnityCommunicatorConfig config;

        public HttpServer(final UnityCommunicatorConfig config) {
            this.config = config;
        }

        private void handleWebsocketHandshake(final ChannelHandlerContext ctx, final FullHttpRequest request) {
            final String wsUrl = getWebsocketUrl(true, request);

            final WebSocketDecoderConfig wsConfig = WebSocketDecoderConfig.newBuilder()
                    .expectMaskedFrames(false)
                    .allowExtensions(true)
                    .allowMaskMismatch(true)
                    .maxFramePayloadLength(1024 * 1024 * 20)
                    .build();

            final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(wsUrl, null, wsConfig);
            final WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(request);

            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                handshaker.handshake(ctx.channel(), request);
            }
        }

        private boolean handleWebsocketUpgrade(final ChannelHandlerContext ctx, final FullHttpRequest request) {
            final HttpHeaders headers = request.headers();

            if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION)) &&
                    "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) {
                ctx.pipeline().replace(this, "websocketHandler", new WebsocketServer(this.config));

                handleWebsocketHandshake(ctx, request);
                return true;
            }

            return false;
        }

        private void handleDefault(final ChannelHandlerContext ctx) {
            final String responseMessage = "Hi from G-Earth, only websocket connections are allowed.";

            final DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.NOT_FOUND,
                    Unpooled.copiedBuffer(responseMessage.getBytes(StandardCharsets.UTF_8)));

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, responseMessage.length());

            ctx.writeAndFlush(response);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof FullHttpRequest) {
                if (handleWebsocketUpgrade(ctx, (FullHttpRequest) msg)) {
                    return;
                }

                handleDefault(ctx);
            } else {
                super.channelRead(ctx, msg);
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.writeAndFlush(new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    Unpooled.copiedBuffer(cause.getMessage().getBytes())
            ));
        }

        private String getWebsocketUrl(final boolean ssl, final HttpRequest request) {
            return String.format("%s://%s%s",
                    ssl ? "wss" : "ws",
                    request.headers().get("Host"),
                    request.uri());
        }
    }

    public static class WebsocketServer extends ChannelInboundHandlerAdapter {

        private static final Logger LOG = LoggerFactory.getLogger(WebsocketServer.class);

        private final HProxySetter proxySetter;
        private final HStateSetter stateSetter;
        private final HConnection hConnection;
        private final ProxyProvider proxyProvider;

        private HProxy hProxy;
        private String revision;

        public WebsocketServer(final UnityCommunicatorConfig config) {
            this.proxySetter = config.proxySetter();
            this.stateSetter = config.stateSetter();
            this.hConnection = config.hConnection();
            this.proxyProvider = config.proxyProvider();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof BinaryWebSocketFrame frame)) {
                super.channelRead(ctx, msg);
                return;
            }

            try {
                if (revision == null) {
                    revision = frame.content().toString(StandardCharsets.ISO_8859_1);
                    return;
                }

                final byte packetDirection = frame.content().readByte();
                final byte[] packet = new byte[frame.content().readableBytes()];

                frame.content().readBytes(packet);

                // Handle proxy.
                if (hProxy == null && packetDirection == 1) {
                    final HPacket clientHello = new HPacket(packet);

                    if (clientHello.getBytesLength() > 6 && clientHello.headerId() == 4000) {
                        hProxy = new HProxy(HClient.UNITY, "", "", -1, -1, "");

                        final String ignore = clientHello.readString();
                        final String clientIdentifier = clientHello.readString();
                        final WebNettySession session = new WebNettySession(ctx.channel());

                        hProxy.verifyProxy(
                                new UnityPacketHandler(hConnection.getExtensionHandler(), hConnection.getTrafficObservables(), session, HMessage.Direction.TOCLIENT),
                                new UnityPacketHandler(hConnection.getExtensionHandler(), hConnection.getTrafficObservables(), session, HMessage.Direction.TOSERVER),
                                revision,
                                clientIdentifier
                        );

                        proxySetter.setProxy(hProxy);
                        stateSetter.setState(HState.CONNECTED);
                    }
                }

                if (hProxy != null && packetDirection == 0) {
                    hProxy.getInHandler().act(packet);
                } else if (hProxy != null && packetDirection == 1) {
                    hProxy.getOutHandler().act(packet);
                } else {
                    proxyProvider.abort();
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOG.error("Exception caught in unity websocket handler", cause);

            ctx.close();
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) {
            proxyProvider.abort();
        }
    }

}
