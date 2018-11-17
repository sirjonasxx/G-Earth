package gearth.extensions;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

/**
 * Created by Jeunez on 17/11/2018.
 */
public interface IExtension {

    boolean sendToClient(HPacket packet);
    boolean sendToServer(HPacket packet);
    void intercept(HMessage.Side side, int headerId, Extension.MessageListener messageListener);
    void intercept(HMessage.Side side, Extension.MessageListener messageListener);
    boolean requestFlags(Extension.FlagsCheckListener flagRequestCallback);
    void writeToConsole(String s); // not implemented in g-earth yet
    void onConnect(Extension.OnConnectionListener listener);

}
