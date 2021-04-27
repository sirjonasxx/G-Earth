package gearth.extensions;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

public abstract class IExtension {

    public abstract boolean sendToClient(HPacket packet);
    public abstract boolean sendToServer(HPacket packet);
    public abstract void intercept(HMessage.Direction direction, int headerId, Extension.MessageListener messageListener);
    public abstract void intercept(HMessage.Direction direction, Extension.MessageListener messageListener);
    public abstract boolean requestFlags(Extension.FlagsCheckListener flagRequestCallback);
    public abstract void writeToConsole(String colorClass, String s);
    public abstract void writeToConsole(String s);
    public abstract void onConnect(OnConnectionListener listener);

    abstract void initExtension();
    abstract void onClick();
    abstract void onStartConnection();
    abstract void onEndConnection();
    abstract ExtensionInfo getInfoAnnotations();
    abstract boolean canLeave();
    abstract boolean canDelete();
}
