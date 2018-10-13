package gearth.protocol;

public interface StateChangeListener {

    void stateChanged(HConnection.State oldState, HConnection.State newState);

}
