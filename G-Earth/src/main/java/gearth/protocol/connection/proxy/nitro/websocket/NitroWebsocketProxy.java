package gearth.protocol.connection.proxy.nitro.websocket;

import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NitroWebsocketProxy extends HttpProxyIntercept {

    private static final Logger LOG = LoggerFactory.getLogger(NitroWebsocketProxy.class);

    private final NitroWebsocketCallback callback;

    public NitroWebsocketProxy(NitroWebsocketCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onWebsocketHandshakeCompleted(HttpProxyInterceptPipeline pipeline) {
        this.callback.onHandshakeComplete();
    }

    @Override
    public void onWebsocketRequest(Channel clientChannel, Channel proxyChannel, WebSocketFrame webSocketFrame, HttpProxyInterceptPipeline pipeline) throws Exception {
        final byte[] data = getBinaryData(webSocketFrame);
        if (data != null) {
            this.callback.onClientMessage(data);
        }

        webSocketFrame.release();
    }

    @Override
    public void onWebsocketResponse(Channel clientChannel, Channel proxyChannel, WebSocketFrame webSocketFrame, HttpProxyInterceptPipeline pipeline) throws Exception {
        final byte[] data = getBinaryData(webSocketFrame);
        if (data != null) {
            this.callback.onServerMessage(data);
        }

        webSocketFrame.release();
    }

    @Override
    public void onWebsocketClose(HttpProxyInterceptPipeline pipeline) {
        this.callback.onClose();
    }

    private byte[] getBinaryData(WebSocketFrame frame) {
        if (frame instanceof BinaryWebSocketFrame binaryFrame) {
            final ByteBuf content = binaryFrame.content();
            final byte[] binaryData = new byte[binaryFrame.content().readableBytes()];

            content.markReaderIndex();

            try {
                content.readBytes(binaryData);
            } finally {
                content.resetReaderIndex();
            }

            return binaryData;
        }

        LOG.error("Unexpected nitro frame type: {}", frame.getClass());
        return null;
    }
}
