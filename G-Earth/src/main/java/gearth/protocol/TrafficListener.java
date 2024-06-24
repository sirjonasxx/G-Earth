package gearth.protocol;

public interface TrafficListener {

    int BEFORE_MODIFICATION = 0;
    int MODIFICATION = 1;
    int AFTER_MODIFICATION = 2;

    void onCapture(HMessage message);

}
