package gearth.protocol.connection.proxy.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebNettySession implements WebSession {

    private static final Logger logger = LoggerFactory.getLogger(WebNettySession.class);

    private final Channel channel;

    public WebNettySession(Channel channel) {
        this.channel = channel;
    }

    @Override
    public boolean send(byte[] buffer) throws IOException {
        if (!this.channel.isActive()) {
            logger.error("Nitro netty channel is closed, cannot send data");
            return false;
        }

        this.channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(buffer)));
        return true;
    }
}
