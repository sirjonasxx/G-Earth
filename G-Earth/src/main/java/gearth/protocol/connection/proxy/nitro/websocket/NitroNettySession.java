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
    private NitroNettyModifier modifier;

    public NitroNettySession(Channel channel) {
        this.channel = channel;
    }

    public void setModifier(NitroNettyModifier modifier) {
        this.modifier = modifier;
    }

    @Override
    public boolean send(byte[] buffer) throws IOException {
        if (!this.channel.isActive()) {
            logger.error("Nitro netty channel is closed, cannot send data");
            return false;
        }

        if (this.modifier != null) {
            try {
                buffer = this.modifier.modify(buffer);
            } catch (Exception e) {
                throw new IOException("Failed to modify data", e);
            }
        }

        this.channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(buffer)));
        return true;
    }
}
