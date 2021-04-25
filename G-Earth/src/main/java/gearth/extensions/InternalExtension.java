package gearth.extensions;

import gearth.Main;
import gearth.extensions.ExtensionBase;
import gearth.extensions.IExtension;
import gearth.protocol.HPacket;


public class InternalExtension extends ExtensionBase {

    public interface InternalExtensionCommunicator {
        void sendToClient(HPacket packet);
        void sendToServer(HPacket packet);
        void writeToConsole(String s);
    }

    private InternalExtensionCommunicator communicator = null;

    public void setCommunicator(InternalExtensionCommunicator communicator) {
        this.communicator = communicator;
    }

    @Override
    public boolean sendToClient(HPacket packet) {
        communicator.sendToClient(packet);
        return true;
    }

    @Override
    public boolean sendToServer(HPacket packet) {
        communicator.sendToServer(packet);
        return true;
    }

    @Override
    public boolean requestFlags(FlagsCheckListener flagRequestCallback) {
        flagRequestCallback.act(Main.args);
        return true;
    }

    @Override
    public void writeToConsole(String colorClass, String s) {
        String text = "[" + colorClass + "]" + getInfoAnnotations().Title() + " --> " + s;
        communicator.writeToConsole(text);
    }

    // to be maybe overwritten
    @Override
    protected void initExtension() { }

    // to be maybe overwritten
    @Override
    protected void onStartConnection() {}

    // to be maybe overwritten
    @Override
    protected void onEndConnection() {}

    @Override
    protected boolean canLeave() {
        return false;
    }

    @Override
    protected boolean canDelete() {
        return false;
    }
}
