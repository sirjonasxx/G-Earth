package gearth.app.protocol.interceptor;

import gearth.app.protocol.connection.HProxy;

import java.io.IOException;
import java.net.Socket;

public interface ConnectionInterceptorCallbacks {

    void onInterceptorConnected(Socket client, Socket server, HProxy proxy) throws IOException, InterruptedException;

    void onInterceptorError();

}
