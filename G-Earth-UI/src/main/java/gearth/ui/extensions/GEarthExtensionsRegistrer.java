package gearth.ui.extensions;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jonas on 21/06/18.
 */
public class GEarthExtensionsRegistrer {

    private ServerSocket serverSocket;

    GEarthExtensionsRegistrer(ExtensionRegisterObserver observer) throws IOException {

//        serverSocket = new ServerSocket(0);
        int port = 9092;
        boolean serverSetup = false;
        while (!serverSetup) {
            serverSetup = createServer(port);
            port++;
        }


        new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket extensionSocket = serverSocket.accept();
                    GEarthExtension.create(extensionSocket, observer::onConnect, observer::onDisconnect);
                }
            } catch (IOException e) {e.printStackTrace();}
        }).start();
    }

    private boolean createServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public interface ExtensionRegisterObserver {
        void onConnect(GEarthExtension extension);
        void onDisconnect(GEarthExtension extension);
    }
}
