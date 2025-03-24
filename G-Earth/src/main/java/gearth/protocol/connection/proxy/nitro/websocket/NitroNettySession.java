package gearth.protocol.connection.proxy.nitro.websocket;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class NitroNettySession implements NitroSession {

    private static final Logger logger = LoggerFactory.getLogger(NitroNettySession.class);

    private final Channel channel;

    public NitroNettySession(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void send(byte[] buffer) throws IOException {
        if (!this.channel.isWritable()) {
            logger.error("Nitro netty channel is closed, cannot send data");
            return;
        }

        this.channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(buffer)));
    }

}
