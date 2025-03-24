package gearth.protocol.connection.proxy.nitro.websocket;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.io.IOException;

public class NitroNettySession implements NitroSession {

    private final Channel channel;

    public NitroNettySession(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void send(byte[] buffer) throws IOException {
        this.channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(buffer)));
    }

}
