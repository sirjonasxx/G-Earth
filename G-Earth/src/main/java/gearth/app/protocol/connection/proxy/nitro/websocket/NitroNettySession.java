package gearth.app.protocol.connection.proxy.nitro.websocket;

import gearth.app.protocol.connection.proxy.http.WebNettySession;
import gearth.app.protocol.connection.proxy.nitro.NitroPacketEvent;
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
        final NitroPacketEvent event = new NitroPacketEvent(buffer);

        if (this.modifier != null) {
            try {
                this.modifier.modify(event);
            } catch (Exception e) {
                throw new IOException("Failed to modify data", e);
            }
        }

        if (event.cancel) {
            return true;
        }

        return super.send(event.buffer);
    }
}
