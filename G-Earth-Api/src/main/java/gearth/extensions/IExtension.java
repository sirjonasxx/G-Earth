package gearth.extensions;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.packet_info.PacketInfoManager;

public abstract class IExtension {

    public abstract boolean sendToClient(HPacket packet);
    public abstract boolean sendToServer(HPacket packet);
    public abstract void intercept(HMessage.Direction direction, int headerId, Extension.MessageListener messageListener);
    public abstract void intercept(HMessage.Direction direction, String hashOrName, Extension.MessageListener messageListener);
    public abstract void intercept(HMessage.Direction direction, Extension.MessageListener messageListener);
    public abstract boolean requestFlags(Extension.FlagsCheckListener flagRequestCallback);
    public abstract void writeToConsole(String colorClass, String s);
    public abstract void writeToConsole(String s);
    public abstract void onConnect(OnConnectionListener listener);

    public abstract void initExtension();
    public abstract void onClick();
    public abstract void onStartConnection();
    public abstract void onEndConnection();
    public abstract ExtensionInfo getInfoAnnotations();
    public abstract boolean canLeave();
    public abstract boolean canDelete();

    public abstract PacketInfoManager getPacketInfoManager();
}
