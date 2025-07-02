package gearth.app.protocol;

import gearth.app.protocol.connection.HState;

public interface StateChangeListener {

    void stateChanged(HState oldState, HState newState);

}
