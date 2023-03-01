package gearth.protocol.connection.proxy.flash;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.memory.Rc4Obtainer;
import gearth.protocol.packethandler.flash.FlashPacketHandler;
import gearth.protocol.packethandler.flash.IncomingFlashPacketHandler;
import gearth.protocol.packethandler.flash.OutgoingFlashPacketHandler;
import gearth.ui.titlebar.TitleBarController;
import gearth.ui.translations.LanguageBundle;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public abstract class FlashProxyProvider implements ProxyProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final HProxySetter proxySetter;
    protected final HStateSetter stateSetter;
    protected final HConnection hConnection;

    private Semaphore abortSemaphore = null;

    public FlashProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection){
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.hConnection = hConnection;
    }

    protected void startProxyThread(Socket client, Socket server, HProxy proxy) throws IOException, InterruptedException {
        final boolean[] datastream = new boolean[1];
        server.setTcpNoDelay(true);
        client.setTcpNoDelay(true);

        client.setSoTimeout(0);
        server.setSoTimeout(0);

        logger.debug("Starting proxy thread at {}:{}", server.getLocalAddress().getHostAddress(), server.getLocalPort());

        Rc4Obtainer rc4Obtainer = new Rc4Obtainer(hConnection);

        OutgoingFlashPacketHandler outgoingHandler = new OutgoingFlashPacketHandler(server.getOutputStream(), hConnection.getTrafficObservables(), hConnection.getExtensionHandler());
        IncomingFlashPacketHandler incomingHandler = new IncomingFlashPacketHandler(client.getOutputStream(), hConnection.getTrafficObservables(), outgoingHandler, hConnection.getExtensionHandler());
        rc4Obtainer.setFlashPacketHandlers(outgoingHandler, incomingHandler);

        Semaphore abort = new Semaphore(0);

        outgoingHandler.addOnDatastreamConfirmedListener((hotelVersion, clientIdentifier) -> {
            incomingHandler.setAsDataStream();
            proxy.verifyProxy(incomingHandler, outgoingHandler, hotelVersion, clientIdentifier);
            proxySetter.setProxy(proxy);
            datastream[0] = true;
            abortSemaphore = abort;
            onConnect();
        });

        handleInputStream(client, outgoingHandler, abort);
        handleInputStream(server, incomingHandler, abort);

        // abort can be acquired as soon as one of the sockets is closed
        abort.acquire();
        try	{
            if (!server.isClosed()) server.close();
            if (!client.isClosed()) client.close();
            logger.debug("Closed server {} and client {}, dataStreams {}", server, client, datastream);
            if (datastream[0]) {
                onConnectEnd();
            };
        }
        catch (IOException e) {
            logger.error("Failed to gracefully stop", e);
        }
    }

    private void handleInputStream(Socket socket, FlashPacketHandler flashPacketHandler, Semaphore abort) {
        new Thread(() -> {
            try {
                int readLength;
                byte[] buffer = new byte[10000];
                while (!socket.isClosed() &&
                        (hConnection.getState() == HState.WAITING_FOR_CLIENT || hConnection.getState() == HState.CONNECTED) &&
                        (readLength = socket.getInputStream().read(buffer)) != -1) {
                    flashPacketHandler.act(Arrays.copyOf(buffer, readLength));
                }
            }
            catch (IOException ignore) {
//                logger.error("Failed to read input stream from socket {}",  socket, ignore);
            } finally {
                abort.release();
            }
        }).start();
    }


    public abstract void start() throws IOException;
    public void abort() {
        if (abortSemaphore != null) {
            abortSemaphore.release();
        }
        else {
            stateSetter.setState(HState.NOT_CONNECTED);
        }
    }

    protected void onConnect() {
        stateSetter.setState(HState.CONNECTED);
    }
    protected void onConnectEnd() {
        proxySetter.setProxy(null);
        abortSemaphore = null;
        stateSetter.setState(HState.NOT_CONNECTED);
    }

    protected void showInvalidConnectionError() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
            alert.getDialogPane().getChildren().add(new Label(LanguageBundle.get("alert.invalidconnection.content")));
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.setResizable(false);
            try {
                TitleBarController.create(alert).showAlert();
            } catch (IOException e) {
                logger.error("Failed to create invalid connection error alert", e);
            }
        });
    }
}
