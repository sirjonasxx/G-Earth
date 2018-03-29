package main.protocol;

public interface StateChangeListener {

    void stateChanged(HConnection.State oldState, HConnection.State newState);

}
