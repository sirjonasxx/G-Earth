package gearth.extensions;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

public interface IExtension {

    boolean sendToClient(HPacket packet);
    boolean sendToServer(HPacket packet);
    void intercept(HMessage.Direction direction, int headerId, Extension.MessageListener messageListener);
    void intercept(HMessage.Direction direction, Extension.MessageListener messageListener);
    boolean requestFlags(Extension.FlagsCheckListener flagRequestCallback);
    void writeToConsole(String colorClass, String s);
    void writeToConsole(String s);
    void onConnect(OnConnectionListener listener);

}
