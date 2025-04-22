package gearth.protocol.connection.proxy.nitro.websocket;

import gearth.protocol.connection.proxy.http.WebNettySession;
import io.netty.channel.Channel;

import java.io.IOException;

public class NitroNettySession extends WebNettySession {
    private NitroNettyModifier modifier;

    public NitroNettySession(Channel channel) {
        super(channel);
    }

    public void setModifier(NitroNettyModifier modifier) {
        this.modifier = modifier;
    }

    @Override
    public boolean send(byte[] buffer) throws IOException {
        if (this.modifier != null) {
            try {
                buffer = this.modifier.modify(buffer);
            } catch (Exception e) {
                throw new IOException("Failed to modify data", e);
            }
        }

        return super.send(buffer);
    }
}
