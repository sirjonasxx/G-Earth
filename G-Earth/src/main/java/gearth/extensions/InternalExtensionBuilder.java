package gearth.extensions;


import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.services.extensionhandler.extensions.ExtensionType;
import gearth.services.extensionhandler.extensions.GEarthExtension;

// wraps internal GEarthExtension class to IExtension interface
// to allow internal extensions that follow the same remote-extensions interface
public class InternalExtensionBuilder extends GEarthExtension {

    private final InternalExtension extension;

    public InternalExtensionBuilder(InternalExtension extension) {
        this.extension = extension;
        extension.setCommunicator(new InternalExtension.InternalExtensionCommunicator() {
            @Override
            public void sendToClient(HPacket packet) {
                sendMessage(HMessage.Direction.TOCLIENT, packet);
            }

            @Override
            public void sendToServer(HPacket packet) {
                sendMessage(HMessage.Direction.TOSERVER, packet);
            }

            @Override
            public void writeToConsole(String s) {
                log(s);
            }
        });
    }

    @Override
    public String getAuthor() {
        return extension.getInfoAnnotations().Author();
    }

    @Override
    public String getDescription() {
        return extension.getInfoAnnotations().Description();
    }

    @Override
    public String getTitle() {
        return extension.getInfoAnnotations().Title();
    }

    @Override
    public String getVersion() {
        return extension.getInfoAnnotations().Version();
    }

    @Override
    public boolean isFireButtonUsed() {
        Class<? extends InternalExtension > c = extension.getClass();

        while (c != InternalExtension.class) {
            try {
                c.getDeclaredMethod("onClick");
                return true;
            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
            }

            c = (Class<? extends InternalExtension>) c.getSuperclass();
        }

        return false;
    }

    @Override
    public boolean isDeleteButtonVisible() {
        return extension.canDelete();
    }

    @Override
    public boolean isLeaveButtonVisible() {
        return extension.canLeave();
    }

    @Override
    public boolean isInstalledExtension() {
        return false;
    }

    @Override
    public void doubleclick() {
        extension.onClick();
    }

    @Override
    public void packetIntercept(HMessage hMessage) {
        extension.modifyMessage(hMessage);
        sendManipulatedPacket(hMessage);
    }

    @Override
    public void provideFlags(String[] flags) {
        // no need
    }

    @Override
    public void connectionStart(String host, int port, String hotelVersion, String clientIdentifier, HClient clientType, PacketInfoManager packetInfoManager) {
        extension.getOnConnectionObservable().fireEvent(l -> l.onConnection(
                host, port, hotelVersion,
                clientIdentifier, clientType, packetInfoManager)
        );
        extension.onStartConnection();
    }

    @Override
    public void connectionEnd() {
        extension.onEndConnection();
    }

    @Override
    public void init(boolean isConnected) {
        extension.initExtension();
    } // not implementing isConnected, only relevant for g-python

    @Override
    public void close() {
        // no need in internal ext
    }

    @Override
    public void packetToStringResponse(String string, String expression) {
        // no need in java ext
    }

    @Override
    public void stringToPacketResponse(HPacket packet) {
        // no need in java ext
    }

    @Override
    public ExtensionType extensionType() {
        return ExtensionType.INTERNAL;
    }

}
