package gearth.services.always_admin;

import gearth.protocol.HConnection;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

public class AdminService {

    private final HConnection hConnection;

    volatile private boolean enabled;
    volatile private HPacket originalPacket;

    public AdminService(boolean enabled, HConnection hConnection) {
        this.enabled = enabled;
        this.hConnection = hConnection;
        originalPacket = null;
    }

    public void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            this.enabled = enabled;
            if (originalPacket != null) {
                if (enabled) {
                    hConnection.sendToClient(new HPacket(originalPacket.headerId(), 7, 7, true));
                }
                else {
                    hConnection.sendToClient(originalPacket);
                }
            }
        }
    }

    public void onConnect() {
        originalPacket = null;
    }

    public void onMessage(HMessage message) {
        HPacket packet = message.getPacket();
        if (message.getDestination() == HMessage.Direction.TOCLIENT
                && (originalPacket == null || packet.headerId() == originalPacket.headerId())
                && packet.length() == 11 && (packet.readByte(14) == 0 || packet.readByte(14) == 1)) {
            originalPacket = new HPacket(packet);

            if (enabled) {
                packet.replaceInt(6, 7);
                packet.replaceInt(10, 7);
                packet.replaceBoolean(14, true);
            }
        }

    }

}
