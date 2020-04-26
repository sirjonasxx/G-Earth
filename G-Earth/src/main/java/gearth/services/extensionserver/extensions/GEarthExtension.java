package gearth.services.extensionserver.extensions;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.beans.InvalidationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
    public abstract void connectionStart(String host, int port, String hotelVersion, String harbleMessagesPath);
    public abstract void connectionEnd();
    public abstract void init();
    public abstract void close();
    // ---------------------------------------------------------------





    // ----------------- listen to the extension ---------------------
    protected final List<ExtensionListener> extensionListeners = new ArrayList<>();
    public void registerExtensionListener(ExtensionListener listener) {
        this.extensionListeners.add(listener);
    }
    public void removeExtensionListener(ExtensionListener listener) {
        this.extensionListeners.remove(listener);
    }
    private void notifyListeners(Consumer<ExtensionListener> consumer) {
        for (int i = extensionListeners.size() - 1; i >= 0; i--) {
            consumer.accept(extensionListeners.get(i));
        }

        extensionListeners.forEach(consumer);
    }

    protected void sendManipulatedPacket(HMessage hMessage) {
        int orgIndex = hMessage.getPacket().getReadIndex();
        notifyListeners(listener -> {
            hMessage.getPacket().setReadIndex(6);
            listener.manipulatedPacket(hMessage);
        });
        hMessage.getPacket().setReadIndex(orgIndex);
    }
    protected void requestFlags() {
        notifyListeners(ExtensionListener::flagsRequest);
    }
    protected void sendMessage(HMessage.Direction direction, HPacket packet) {
        int orgIndex = packet.getReadIndex();
        notifyListeners(listener -> {
            packet.setReadIndex(6);
            listener.sendMessage(direction, packet);
        });
        packet.setReadIndex(orgIndex);
    }
    protected void log(String text) {
        notifyListeners(listener -> listener.log(text));
    }
    protected void hasClosed() {
        notifyListeners(ExtensionListener::hasClosed);
    }
    // --------------------------------------------------------------------







    // ----------- methods for interaction with G-Earth UI, don't use/change them ----------------

    private final List<InvalidationListener> onRemoveClickListener = new ArrayList<>();
    public void onRemoveClick(InvalidationListener listener) {
        synchronized (onRemoveClickListener) {
            onRemoveClickListener.add(listener);
        }
    }
    public void isRemoveClickTrigger() {
        synchronized (onRemoveClickListener) {
            for (int i = onRemoveClickListener.size() - 1; i >= 0; i--) {
                onRemoveClickListener.get(i).invalidated(null);
            }
        }
    }

    private final List<InvalidationListener> onClickListener = new ArrayList<>();
    public void onClick(InvalidationListener listener) {
        synchronized (onClickListener) {
            onClickListener.add(listener);
        }
    }
    public void isClickTrigger() {
        synchronized (onClickListener) {
            for (int i = onClickListener.size() - 1; i >= 0; i--) {
                onClickListener.get(i).invalidated(null);
            }
        }
    }

    private final List<InvalidationListener> onDeleteListeners = new ArrayList<>();
    public void onDelete(InvalidationListener listener) {
        synchronized (onDeleteListeners) {
            onDeleteListeners.add(listener);
        }
    }
    public void delete() {
        synchronized (onDeleteListeners) {
            for (int i = onDeleteListeners.size() - 1; i >= 0; i--) {
                onDeleteListeners.get(i).invalidated(null);
            }
        }
    }
    // ----------------------------------------------------------------------------------------

}
