package gearth.protocol.connection.proxy;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.memory.Rc4Obtainer;
import gearth.protocol.packethandler.IncomingPacketHandler;
import gearth.protocol.packethandler.OutgoingPacketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public abstract class ProxyProvider {

    protected final HProxySetter proxySetter;
    protected final HStateSetter stateSetter;
    protected final HConnection hConnection;

    public ProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection){
        this.proxySetter = proxySetter;
        this.stateSetter = stateSetter;
        this.hConnection = hConnection;
    }

    protected void startProxyThread(Socket client, Socket server, HProxy proxy) throws InterruptedException, UnknownHostException, IOException {
        final boolean[] datastream = new boolean[1];
        server.setTcpNoDelay(true);

        OutputStream client_out = client.getOutputStream();
        InputStream client_in = client.getInputStream();
        OutputStream habbo_server_out = server.getOutputStream();
        InputStream habbo_server_in = server.getInputStream();

        if (HConnection.DEBUG) System.out.println(server.getLocalAddress().getHostAddress() + ": " + server.getLocalPort());

        final boolean[] aborted = new boolean[1];
        Rc4Obtainer rc4Obtainer = new Rc4Obtainer(hConnection);

        OutgoingPacketHandler outgoingHandler = new OutgoingPacketHandler(habbo_server_out, hConnection.getTrafficObservables());
        IncomingPacketHandler incomingHandler = new IncomingPacketHandler(client_out, hConnection.getTrafficObservables());
        rc4Obtainer.setPacketHandlers(outgoingHandler, incomingHandler);

        outgoingHandler.addOnDatastreamConfirmedListener(hotelVersion -> {
            incomingHandler.setAsDataStream();
            proxy.verifyProxy(incomingHandler, outgoingHandler, hotelVersion);
            proxySetter.setProxy(proxy);
            datastream[0] = true;
            onConnect();
        });

        // wachten op data van client
        new Thread(() -> {
            try {
                while (!client.isClosed() && (hConnection.getState() == HState.WAITING_FOR_CLIENT || hConnection.getState() == HState.CONNECTED)) {
                    byte[] buffer;
                    while (client_in.available() > 0)	{
                        client_in.read(buffer = new byte[client_in.available()]);
                        outgoingHandler.act(buffer);
                    }
                    Thread.sleep(1);

                }
            }
            catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                if (HConnection.DEBUG) System.out.println("abortclient");
                try {
                    if (habbo_server_out != null) habbo_server_out.close();
                    if (habbo_server_in != null) habbo_server_in.close();
                    if (client_in != null) client_in.close();
                    if (client_out != null) client_out.close();
                    if (server != null && !server.isClosed()) server.close();
                    if (client != null && !client.isClosed()) client.close();
                    aborted[0] = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (datastream[0]) {
                    onConnectEnd();
                };
            }
        }).start();
        // wachten op data van server
        new Thread(() -> {
            try {
                while (!server.isClosed() && (hConnection.getState() == HState.WAITING_FOR_CLIENT || hConnection.getState() == HState.CONNECTED)) {
                    byte[] buffer;
                    while (habbo_server_in.available() > 0) {
                        habbo_server_in.read(buffer = new byte[habbo_server_in.available()]);
                        incomingHandler.act(buffer);
                    }
                    Thread.sleep(1);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (habbo_server_out != null) habbo_server_out.close();
                    if (habbo_server_in != null) habbo_server_in.close();
                    if (client_in != null) client_in.close();
                    if (client_out != null) client_out.close();
                    if (!server.isClosed()) server.close();
                    if (!client.isClosed()) client.close();
                    aborted[0] = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        while(!aborted[0]) {
            Thread.sleep(50);
        }

        try	{
            if (!server.isClosed()) server.close();
            if (!client.isClosed()) client.close();
            if (HConnection.DEBUG) System.out.println("STOP");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public abstract void start() throws IOException;
    public abstract void abort();

    protected void onConnect() {
        stateSetter.setState(HState.CONNECTED);
    }
    protected void onConnectEnd() {
        proxySetter.setProxy(null);
        stateSetter.setState(HState.NOT_CONNECTED);
    }


}
