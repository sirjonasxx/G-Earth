package main.ui.extensions;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jonas on 21/06/18.
 */
public class GEarthExtensionsRegistrer {

    final ServerSocket serverSocket;

    GEarthExtensionsRegistrer(ExtensionRegisterObserver observer) throws IOException {

        serverSocket = new ServerSocket(0);
        new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket extensionSocket = serverSocket.accept();
                    GEarthExtension.create(extensionSocket, observer::onConnect, observer::onDisconnect);
                }
            } catch (IOException e) {e.printStackTrace();}
        }).start();
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public interface ExtensionRegisterObserver {
        void onConnect(GEarthExtension extension);
        void onDisconnect(GEarthExtension extension);
    }
}
