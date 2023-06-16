package gearth.protocol.connection.proxy.nitro;

import gearth.protocol.HMessage;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NitroConnectionState {

    private static final Logger logger = LoggerFactory.getLogger(NitroConnectionState.class);

    private final HStateSetter stateSetter;

    private boolean aborting;
    private boolean toServer;
    private boolean toClient;

    public NitroConnectionState(HStateSetter stateSetter) {
        this.stateSetter = stateSetter;
    }

    public void setConnected(HMessage.Direction direction) {
        if (direction == HMessage.Direction.TOCLIENT) {
            this.toClient = true;
        } else if (direction == HMessage.Direction.TOSERVER) {
            this.toServer = true;
        }

        this.checkConnected();
    }

    public boolean isConnected() {
        if (this.aborting) {
            return false;
        }

        if (!this.toClient) {
            return false;
        }

        if (!this.toServer) {
            return false;
        }

        return true;
    }

    private void checkConnected() {
        if (!this.isConnected()) {
            return;
        }

        this.stateSetter.setState(HState.CONNECTED);

        logger.info("Connected");
    }

    public void setAborting() {
        this.aborting = true;
        this.stateSetter.setState(HState.ABORTING);

        logger.info("Aborting");
    }

}
