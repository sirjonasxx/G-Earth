package gearth.app.services.nitro.hotels;

import gearth.app.protocol.connection.proxy.nitro.NitroPacketEvent;
import gearth.app.services.nitro.NitroHotel;
import gearth.app.services.nitro.NitroPacketModifier;

import java.nio.ByteBuffer;
import java.util.Collections;

public class LeetNL extends NitroHotel {

    public LeetNL() {
        super("leet.city",
                Collections.singletonList("wss://proxy.leet.city/"),
                Collections.emptyList());
    }

    @Override
    public NitroPacketModifier createPacketModifier() {
        return new LeetNLPacketModifier();
    }

    @Override
    protected void loadAsset(String host, String uri, byte[] data) {
    }

    public static class LeetNLPacketModifier implements NitroPacketModifier {

        private static final short OUTGOING_FIRST = 4000;
        private static final short INCOMING_FIRST = 1347;

        private final DirectionStateHolder client;
        private final DirectionStateHolder server;

        public LeetNLPacketModifier() {
            client = new DirectionStateHolder(OUTGOING_FIRST);
            server = new DirectionStateHolder(INCOMING_FIRST);
        }

        private void toGearth(final DirectionStateHolder holder, final NitroPacketEvent e) {
            if (holder.state == DirectionState.BOOTSTRAP) {
                holder.state = DirectionState.HANDSHAKE;
                e.bypass = true;
                return;
            }

            final ByteBuffer payload = ByteBuffer.wrap(e.buffer.clone());

            if (holder.state == DirectionState.HANDSHAKE) {
                holder.state = DirectionState.CONNECTED;
                holder.offset = (short) (payload.getShort(4) - holder.firstId);
            }

            payload.putShort(4, (short) (payload.getShort(4) - holder.offset));

            e.buffer = payload.array();
        }

        private void fromGearth(final DirectionStateHolder holder, final NitroPacketEvent e) {
            if (holder.state != DirectionState.CONNECTED) {
                return;
            }

            final ByteBuffer payload = ByteBuffer.wrap(e.buffer.clone());

            payload.putShort(4, (short) (payload.getShort(4) + holder.offset));

            e.buffer = payload.array();
        }

        @Override public void clientToGearth(final NitroPacketEvent e) { toGearth(client, e); }
        @Override public void serverToGearth(final NitroPacketEvent e) { toGearth(server, e); }
        @Override public void gearthToClient(final NitroPacketEvent e) { fromGearth(server, e); }
        @Override public void gearthToServer(final NitroPacketEvent e) { fromGearth(client, e); }

        private static final class DirectionStateHolder {
            final short firstId;
            DirectionState state;
            short offset;

            public DirectionStateHolder(short firstId) {
                this.state = DirectionState.BOOTSTRAP;
                this.firstId = firstId;
            }
        }

        private enum DirectionState {
            BOOTSTRAP,
            HANDSHAKE,
            CONNECTED
        }
    }
}
