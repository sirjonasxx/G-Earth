package gearth.services.extensionhandler.extensions;

import gearth.misc.listenerpattern.Observable;
import gearth.misc.listenerpattern.SynchronizedObservable;
import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.services.extensionhandler.extensions.listeners.OmRemoveClickListener;
import gearth.services.extensionhandler.extensions.listeners.OnClickListener;
import gearth.services.extensionhandler.extensions.listeners.OnDeleteListener;

public abstract class GEarthExtension {


    // ------ static extension information --------
    public abstract String getAuthor();
    public abstract String getDescription();
    public abstract String getTitle();
    public abstract String getVersion();

    public String getFileName() {
        return ""; // override in extensions over network if executed from file
    }

    public abstract boolean isFireButtonUsed();
    public abstract boolean isDeleteButtonVisible();
    public abstract boolean isLeaveButtonVisible();
    public abstract boolean isInstalledExtension();
    // --------------------------------------------






    // ------- actions you can perform towards the extension ---------
    public abstract void doubleclick();
    public abstract void packetIntercept(HMessage hMessage);
    public abstract void provideFlags(String[] flags);
    public abstract void connectionStart(String host, int port, String hotelVersion, String clientIdentifier, HClient clientType, PacketInfoManager packetInfoManager);
    public abstract void connectionEnd();
    public abstract void init(boolean isConnected);
    public abstract void close();
    public abstract void packetToStringResponse(String string, String expression);
    public abstract void stringToPacketResponse(HPacket packet);
    // ---------------------------------------------------------------





    // ----------------- listen to the extension ---------------------
    protected final Observable<ExtensionListener> extensionObservable = new SynchronizedObservable<>();
    public Observable<ExtensionListener> getExtensionObservable() {
        return extensionObservable;
    }

    protected void sendManipulatedPacket(HMessage hMessage) {
        int orgIndex = hMessage.getPacket().getReadIndex();
        extensionObservable.fireEvent(listener -> {
            hMessage.getPacket().setReadIndex(6);
            listener.manipulatedPacket(hMessage);
        });
        hMessage.getPacket().setReadIndex(orgIndex);
    }
    protected void requestFlags() {
        extensionObservable.fireEvent(ExtensionListener::flagsRequest);
    }
    protected void sendMessage(HMessage.Direction direction, HPacket packet) {
        int orgIndex = packet.getReadIndex();
        extensionObservable.fireEvent(listener -> {
            packet.setReadIndex(6);
            listener.sendMessage(direction, packet);
        });
        packet.setReadIndex(orgIndex);
    }
    protected void log(String text) {
        extensionObservable.fireEvent(listener -> listener.log(text));
    }
    protected void hasClosed() {
        extensionObservable.fireEvent(ExtensionListener::hasClosed);
    }

    protected void packetToStringRequest(HPacket packet) {
        int orgIndex = packet.getReadIndex();
        extensionObservable.fireEvent(listener -> {
            packet.setReadIndex(6);
            listener.packetToStringRequest(packet);
        });
        packet.setReadIndex(orgIndex);
    }
    protected void stringToPacketRequest(String string) {
        extensionObservable.fireEvent(l -> l.stringToPacketRequest(string));
    }

    // --------------------------------------------------------------------







    // ----------- methods for interaction with G-Earth UI, don't use/change them ----------------

    private final Observable<OmRemoveClickListener> removeClickObservable = new SynchronizedObservable<>(OmRemoveClickListener::onRemove);
    public Observable<OmRemoveClickListener> getRemoveClickObservable() {
        return removeClickObservable;
    }

    private final Observable<OnClickListener> clickedObservable = new SynchronizedObservable<>(OnClickListener::onClick);
    public Observable<OnClickListener> getClickedObservable() {
        return clickedObservable;
    }

    private final Observable<OnDeleteListener> deletedObservable = new SynchronizedObservable<>(OnDeleteListener::onDelete);
    public Observable<OnDeleteListener> getDeletedObservable() {
        return deletedObservable;
    }
    // ----------------------------------------------------------------------------------------



    public abstract ExtensionType extensionType();
}
