package gearth.protocol.connection.proxy.shockwave;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HClient;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.interceptor.ConnectionInterceptor;
import gearth.protocol.interceptor.ConnectionInterceptorCallbacks;
import gearth.protocol.memory.Rc4Obtainer;
import gearth.protocol.packethandler.PacketHandler;
import gearth.protocol.packethandler.shockwave.ShockwavePacketIncomingHandler;
import gearth.protocol.packethandler.shockwave.ShockwavePacketOutgoingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ShockwaveProxy implements ProxyProvider, ConnectionInterceptorCallbacks {

    private static final Logger logger = LoggerFactory.getLogger(ShockwaveProxy.class);

    private final HProxySetter hProxySetter;
    private final HStateSetter hStateSetter;
    private final HConnection hConnection;
    private final ConnectionInterceptor interceptor;
    private final Semaphore abortSemaphore;

    public ShockwaveProxy(HProxySetter hProxySetter, HStateSetter hStateSetter, HConnection hConnection, List<String> potentialHosts) {
        this.hProxySetter = hProxySetter;
        this.hStateSetter = hStateSetter;
        this.hConnection = hConnection;
        this.interceptor = new ConnectionInterceptor(HClient.SHOCKWAVE, hStateSetter, hConnection, this, potentialHosts, false);
        this.abortSemaphore = new Semaphore(0);
    }

    @Override
    public void start() throws IOException {
        if (hConnection.getState() != HState.NOT_CONNECTED) {
            return;
        }

        interceptor.start();
        logger.info("Intercepting shockwave connection.");
    }

    @Override
    public void abort() {
        logger.warn("Aborting shockwave proxy.");
        hStateSetter.setState(HState.ABORTING);
        interceptor.stop(false);
        abortSemaphore.release();

        // Let the stopProxyThread handle the rest of the aborting.
        if (hConnection.getState() != HState.CONNECTED) {
            hStateSetter.setState(HState.NOT_CONNECTED);
        }
    }

    @Override
    public void onInterceptorConnected(Socket client, Socket server, HProxy proxy) throws IOException, InterruptedException {
        logger.info("Shockwave connection has been intercepted, starting proxy thread.");

        startProxyThread(client, server, proxy);

        logger.info("Stopped proxy thread.");
    }

    @Override
    public void onInterceptorError() {
        logger.error("Error occurred while intercepting shockwave connection. Aborting.");

        abort();
    }

    private void startProxyThread(Socket client, Socket server, HProxy proxy) throws IOException, InterruptedException {
        server.setSoTimeout(0);
        server.setTcpNoDelay(true);

        client.setSoTimeout(0);
        client.setTcpNoDelay(true);

        logger.info("Connected to shockwave server {}:{}", server.getInetAddress().getHostAddress(), server.getPort());

        final Semaphore abort = new Semaphore(0);

        final ShockwavePacketOutgoingHandler outgoingHandler = new ShockwavePacketOutgoingHandler(server.getOutputStream(), hConnection.getExtensionHandler(), hConnection.getTrafficObservables());
        final ShockwavePacketIncomingHandler incomingHandler = new ShockwavePacketIncomingHandler(client.getOutputStream(), hConnection.getExtensionHandler(), hConnection.getTrafficObservables(), outgoingHandler);

        final Rc4Obtainer rc4Obtainer = new Rc4Obtainer(hConnection);

        rc4Obtainer.setFlashPacketHandlers(outgoingHandler, incomingHandler);
        // TODO: Non hardcoded version "42 Not exactly sure yet how to deal with this for now.
        // Lets revisit when origins is more mature.
        proxy.verifyProxy(incomingHandler, outgoingHandler, "105", "SHOCKWAVE");
        hProxySetter.setProxy(proxy);
        onConnect();

        handleInputStream(client, outgoingHandler, abort);
        handleInputStream(server, incomingHandler, abort);

        // abort can be acquired as soon as one of the sockets is closed
        abort.acquire();

        try {
            if (!server.isClosed()) server.close();
            if (!client.isClosed()) client.close();
            onConnectEnd();
        } catch (IOException e) {
            logger.error("Error occurred while closing sockets.", e);
        }
    }

    private void handleInputStream(Socket socket, PacketHandler packetHandler, Semaphore abort) {
        new Thread(() -> {
            try {
                int readLength;
                byte[] buffer = new byte[8192];
                while (!socket.isClosed() &&
                        (hConnection.getState() == HState.WAITING_FOR_CLIENT || hConnection.getState() == HState.CONNECTED) &&
                        (readLength = socket.getInputStream().read(buffer)) != -1) {
                    packetHandler.act(Arrays.copyOf(buffer, readLength));
                }
            } catch (IOException ignore) {
                // ignore
            } finally {
                abort.release();
            }
        }).start();
    }

    private void onConnect() {
        interceptor.stop(true);
        hStateSetter.setState(HState.CONNECTED);
    }

    private void onConnectEnd() {
        hProxySetter.setProxy(null);
        hStateSetter.setState(HState.NOT_CONNECTED);
    }
}
