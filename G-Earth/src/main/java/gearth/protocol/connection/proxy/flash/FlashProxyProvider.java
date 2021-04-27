package gearth.protocol.connection.proxy.flash;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.memory.Rc4Obtainer;
import gearth.protocol.packethandler.flash.IncomingFlashPacketHandler;
import gearth.protocol.packethandler.flash.OutgoingFlashPacketHandler;
import gearth.protocol.packethandler.flash.FlashPacketHandler;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public abstract class FlashProxyProvider implements ProxyProvider {

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

        if (HConnection.DEBUG) System.out.println(server.getLocalAddress().getHostAddress() + ": " + server.getLocalPort());
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
            if (HConnection.DEBUG) System.out.println("STOP");
            if (datastream[0]) {
                onConnectEnd();
            };
        }
        catch (IOException e) {
            e.printStackTrace();
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
//                System.err.println(packetHandler instanceof IncomingPacketHandler ? "incoming" : "outgoing");
//                ignore.printStackTrace();
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
            Alert alert = new Alert(Alert.AlertType.ERROR, "You entered invalid connection information, G-Earth could not connect", ButtonType.OK);
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.setResizable(false);
            alert.show();
        });
    }

}
