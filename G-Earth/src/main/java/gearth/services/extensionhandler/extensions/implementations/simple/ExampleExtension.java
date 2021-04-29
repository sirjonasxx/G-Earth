package gearth.services.extensionhandler.extensions.implementations.simple;

import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.services.extensionhandler.extensions.ExtensionType;
import gearth.services.extensionhandler.extensions.GEarthExtension;

public class ExampleExtension extends GEarthExtension {
    @Override
    public String getAuthor() {
        return "sirjonasxx";
    }

    @Override
    public String getDescription() {
        return "example internal extension";
    }

    @Override
    public String getTitle() {
        return "Example";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

    @Override
    public boolean isFireButtonUsed() {
        return false;
        // with this button, you could for example open an UI, or use it if your extension has a single purpose
        // that needs a click to be executed

        // will only be visible if you return True here
    }

    @Override
    public boolean isDeleteButtonVisible() {
        return false;

        // can you delete this extension? Can be useful to disable this if you want to provide a built-in tool
        // in G-Earth
    }

    @Override
    public boolean isLeaveButtonVisible() {
        return false;

        // can you disconnect from the extension? (will be connected again when re-opening G-Earth or clicking the
        // "refresh" button in the Extensions GUI)
    }

    @Override
    public boolean isInstalledExtension() {
        return false;

        // is this an extension that is located under the /Extensions folder?
    }

    @Override
    public void doubleclick() {
        System.out.println("wtf dont click me");
    }

    private void intercept(HMessage message) {
        message.getPacket().replaceAllStrings(
                "What is this extension?",
                "It's an example extension showing how extensions work internally"
        );
    }

    @Override
    public void packetIntercept(HMessage hMessage) {
        intercept(hMessage);
        // every packetIntercept needs to be responded with the manipulated version, even if you didn't change anything
        sendManipulatedPacket(hMessage);
    }

    @Override
    public void provideFlags(String[] flags) {
        // If you call "requestFlags()", this function will provide you the execution flags for G-Earth
        // in case you want to do something with it
    }

    @Override
    public void connectionStart(String host, int port, String hotelVersion, String clientIdentifier, HClient clientType, PacketInfoManager packetInfoManager) {
        // a new habbo client has connected
        System.out.println("Connected to " + host);
    }

    @Override
    public void connectionEnd() {
        // the habbo connection has ended
    }

    @Override
    public void init(boolean isConnected) {
        System.out.println("Example extension is connected to G-Earth");
        // the extension is now connected with G-Earth
    }

    @Override
    public void close() {
        // if this function is called, its a heads up that you should close your extension

        // finish up and call "hasClosed()"
        hasClosed();
    }




    // ignore these
    @Override
    public void packetToStringResponse(String string, String expression) {

    }

    @Override
    public void stringToPacketResponse(HPacket packet) {

    }

    @Override
    public ExtensionType extensionType() {
        return ExtensionType.INTERNAL;
    }
}
