package gearth.extensions;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.concurrent.Semaphore;

/**
 * Created by Jonas on 22/09/18.
 */
public abstract class ExtensionForm {

    volatile Extension extension;
    volatile Stage primaryStage;

    protected static void runExtensionForm(String[] args, Class<? extends ExtensionForm> extension) {
        ExtensionFormLauncher launcher = new ExtensionFormLauncher();
        launcher.trigger(extension, args);
    }


    public abstract ExtensionForm launchForm(Stage primaryStage) throws Exception;

    //wrap extension methods
    protected boolean requestFlags(Extension.FlagsCheckListener flagRequestCallback){
        return extension.requestFlags(flagRequestCallback);
    }
    protected void writeToConsole(String s) {
        extension.writeToConsole(s);
    }
    protected void intercept(HMessage.Side side, Extension.MessageListener messageListener) {
        extension.intercept(side, messageListener);
    }
    protected void intercept(HMessage.Side side, int headerId, Extension.MessageListener messageListener){
        extension.intercept(side, headerId, messageListener);
    }
    protected boolean sendToServer(HPacket packet){
        return extension.sendToServer(packet);
    }
    protected boolean sendToClient(HPacket packet){
        return extension.sendToClient(packet);
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
    protected void onClick(){
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
}
