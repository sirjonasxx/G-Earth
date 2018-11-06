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
public abstract class ExtensionForm extends Application {

    private volatile Extension extension;
    protected static String[] args;
    protected volatile Stage primaryStage;

    private volatile ExtensionForm realForm = null;

    @Override
    public void start(Stage primaryStage) throws Exception {
        ExtensionInfo extInfo = getClass().getAnnotation(ExtensionInfo.class);

        realForm = launchForm(primaryStage);
        realForm.extension = new Extension(args) {
            @Override
            protected void init() {
                realForm.initExtension();
            }

            @Override
            protected void onClick() {
                realForm.onClick();
            }

            @Override
            protected void onStartConnection() {
                realForm.onStartConnection();
            }

            @Override
            protected void onEndConnection() {
                realForm.onEndConnection();
            }

            @Override
            ExtensionInfo getInfoAnnotations() {
                return extInfo;
            }
        };
        realForm.primaryStage = primaryStage;
        Thread t = new Thread(() -> {
            realForm.extension.run();
//            Platform.runLater(primaryStage::close);
            //when the extension has ended, close this process
            System.exit(0);
        });
        t.start();

        Platform.setImplicitExit(false);

        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            Platform.runLater(() -> {
                primaryStage.hide();
                realForm.onHide();
            });
        });
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
    private void onClick(){
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
}
