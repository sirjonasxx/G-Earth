package gearth.protocol;

import gearth.protocol.format.shockwave.ShockMessage;

public interface TrafficListener {

    void onCapture(HMessage message);

    void onCapture(ShockMessage message);

}
