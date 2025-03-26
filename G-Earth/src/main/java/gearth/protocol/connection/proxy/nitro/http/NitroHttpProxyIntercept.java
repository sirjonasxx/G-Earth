package gearth.protocol.connection.proxy.nitro.http;

import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.protocol.connection.proxy.nitro.websocket.NitroWebsocketCallback;
import gearth.protocol.connection.proxy.nitro.websocket.NitroWebsocketProxy;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NitroHttpProxyIntercept extends HttpProxyInterceptInitializer {

    private static final Logger log = LoggerFactory.getLogger(NitroHttpProxyIntercept.class);

    private static final int CLIENT_HELLO_PACKET_ID = 4000;

    private final NitroWebsocketCallback callback;

    public NitroHttpProxyIntercept(NitroWebsocketCallback callback) {
        this.callback = callback;
    }

    @Override
    public void init(HttpProxyInterceptPipeline pipeline) {
        pipeline.addLast(new HttpProxyIntercept() {
            @Override
            public void onWebsocketHandshakeCompleted(HttpProxyInterceptPipeline pipeline) {
                callback.onHandshakeComplete();
            }

            @Override
            public void onWebsocketRequest(Channel clientChannel, Channel proxyChannel, WebSocketFrame webSocketFrame, HttpProxyInterceptPipeline pipeline) throws Exception {
                boolean foundMatch = false;

                try {
                    if (!(webSocketFrame instanceof BinaryWebSocketFrame)) {
                        return;
                    }

                    // Obtain url.
                    final String websocketUrl = pipeline.getRequestProto().getWebsocketUrl();

                    log.debug("Checking websocket url: {}", websocketUrl);

                    // Read binary data.
                    final BinaryWebSocketFrame binaryFrame = (BinaryWebSocketFrame) webSocketFrame;
                    final ByteBuf content = binaryFrame.content();
                    final byte[] binaryData = new byte[binaryFrame.content().readableBytes()];

                    content.markReaderIndex();

                    try {
                        content.readBytes(binaryData);
                    } finally {
                        content.resetReaderIndex();
                    }

                    // Log the packet.
                    log.debug("Received binary frame");
                    log.debug(ByteBufUtil.hexDump(binaryFrame.content()));

                    // Detect nitro connection.
                    final HPacket packet = HPacketFormat.EVA_WIRE.createPacket(binaryData);

                    packet.setReadIndex(0);

                    // Check packet length.
                    final int packetLen = packet.readInteger();
                    if (packetLen + 4 != binaryData.length) {
                        log.debug("websocket[{}] packet length mismatch: {} != {}", websocketUrl, packetLen + 4, binaryData.length);
                        return;
                    }

                    // Check packet id.
                    final short packetId = packet.readShort();
                    if (packetId != CLIENT_HELLO_PACKET_ID) {
                        log.debug("websocket[{}] packet id mismatch: {} != {}", websocketUrl, packetId, CLIENT_HELLO_PACKET_ID);
                        return;
                    }

                    log.debug("websocket[{}] found nitro hotel", websocketUrl);

                    foundMatch = true;

                    pipeline.remove(this);
                    pipeline.addLast(new NitroWebsocketProxy(callback));

                    callback.onConnected(clientChannel, proxyChannel);
                    callback.onClientMessage(binaryData);
                } catch (Exception e) {
                    log.error("Failed to read initial binary websocket frame", e);
                } finally {
                    pipeline.remove(this);
                    pipeline.resetWebsocketRequest();

                    if (foundMatch) {
                        webSocketFrame.release();
                    } else {
                        super.onWebsocketRequest(clientChannel, proxyChannel, webSocketFrame, pipeline);
                    }
                }
            }
        });
    }
}
