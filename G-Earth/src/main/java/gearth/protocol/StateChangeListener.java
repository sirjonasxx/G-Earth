package gearth.protocol;

import gearth.protocol.connection.HState;

public interface StateChangeListener {

    void stateChanged(HState oldState, HState newState);

}
