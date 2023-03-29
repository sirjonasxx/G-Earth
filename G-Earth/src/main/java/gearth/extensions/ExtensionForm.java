package gearth.extensions;

import gearth.misc.HostInfo;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.services.packet_info.PacketInfoManager;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Created by Jonas on 22/09/18.
 */
public abstract class ExtensionForm extends ExtensionBase {

    volatile ExtensionBase extension;
    protected volatile Stage primaryStage;
    volatile HostServices hostServices;

    //wrap extension methods
    public boolean requestFlags(Extension.FlagsCheckListener flagRequestCallback){
        return extension.requestFlags(flagRequestCallback);
    }
    public void writeToConsole(String s) {
        extension.writeToConsole(s);
    }
    public void writeToConsole(String colorClass, String s) {
        extension.writeToConsole(colorClass, s);
    }
    public void intercept(HMessage.Direction direction, Extension.MessageListener messageListener) {
        extension.intercept(direction, messageListener);
    }
    public void intercept(HMessage.Direction direction, String hashOrName, Extension.MessageListener messageListener){
        extension.intercept(direction, hashOrName, messageListener);
    }
    public void intercept(HMessage.Direction direction, int headerId, Extension.MessageListener messageListener){
        extension.intercept(direction, headerId, messageListener);
    }
    public boolean sendToServer(HPacket packet){
        return extension.sendToServer(packet);
    }
    public boolean sendToClient(HPacket packet){
        return extension.sendToClient(packet);
    }
    public void onConnect(OnConnectionListener listener) {
        extension.onConnect(listener);
    }

    @Override
    public PacketInfoManager getPacketInfoManager() {
        return extension.getPacketInfoManager();
    }

    protected void onShow(){};
    protected void onHide(){};

    /**
     * Gets called when a connection has been established with G-Earth.
     * This does not imply a connection with Habbo is setup.
     */
    protected void initExtension(){}

    /**
     * The application got doubleclicked from the G-Earth interface. Doing something here is optional
     */
    public void onClick(){
        Platform.runLater(() -> {
            primaryStage.show();
            primaryStage.requestFocus();
            primaryStage.toFront();
            onShow();
        });
    }

    /**
     * A connection with Habbo has been started
     */
    protected void onStartConnection(){}

    /**
     * A connection with Habbo has ended
     */
    protected void onEndConnection(){}

    protected boolean canLeave() {
        return true;
    }

    protected boolean canDelete() {
        return true;
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    public HostInfo getHostInfo() {
        return extension.observableHostInfo.getObject();
    }

    Observable<Runnable> fieldsInitialized = new Observable<>(Runnable::run);
}
