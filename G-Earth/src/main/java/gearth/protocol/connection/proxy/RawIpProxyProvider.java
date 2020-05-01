package gearth.protocol.connection.proxy;

import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.hostreplacer.ipmapping.IpMapper;
import gearth.protocol.hostreplacer.ipmapping.IpMapperFactory;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

public class RawIpProxyProvider extends ProxyProvider {

    private volatile String input_host;
    private volatile int input_port;

    private IpMapper ipMapper = IpMapperFactory.get();
    private boolean hasMapped = false;

    private HProxy proxy = null;

    public RawIpProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, String input_host, int input_port) {
        super(proxySetter, stateSetter, hConnection);
        this.input_host = input_host;
        this.input_port = input_port;
    }

    @Override
    public void start() {
        if (hConnection.getState() != HState.NOT_CONNECTED) {
            return;
        }

        launchMITM();
    }

    private void launchMITM() {
        new Thread(() -> {
            try  {
                stateSetter.setState(HState.PREPARING);
                proxy = new HProxy(input_host, input_host, input_port, input_port, "0.0.0.0");

                Queue<Socket> preConnectedServerConnections = new LinkedList<>();
                for (int i = 0; i < 3; i++) {
                    Socket s1 = new Socket();
                    s1.setSoTimeout(1200);
                    try {
                        s1.connect(new InetSocketAddress(proxy.getActual_domain(), proxy.getActual_port()), 1200);
                    }
                    catch (SocketTimeoutException e) {
                        stateSetter.setState(HState.NOT_CONNECTED);
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "You entered invalid connection information, G-Earth could not connect", ButtonType.OK);
                            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                            alert.setResizable(false);
                            alert.show();
                        });
                        return;
                    }

                    preConnectedServerConnections.add(s1);
                    Thread.sleep(50);
                }

                ipMapper.enable();
                ipMapper.addMapping(proxy.getActual_domain());
                hasMapped = true;

                if (HConnection.DEBUG) System.out.println("Added mapping for raw IP");

                ServerSocket proxy_server = new ServerSocket(proxy.getIntercept_port(), 10, InetAddress.getByName(proxy.getIntercept_host()));
                proxy.initProxy(proxy_server);

                stateSetter.setState(HState.WAITING_FOR_CLIENT);



                while ((hConnection.getState() == HState.WAITING_FOR_CLIENT) && !proxy_server.isClosed())	{
                    try {
                        if (HConnection.DEBUG) System.out.println("try accept proxy");
                        Socket client = proxy_server.accept();

                        if (HConnection.DEBUG) System.out.println("accepted a proxy");

                        new Thread(() -> {
                            try {
                                if (preConnectedServerConnections.isEmpty()) {
                                    if (HConnection.DEBUG) System.out.println("pre-made server connections ran out of stock");
                                }
                                else {
                                    startProxyThread(client, preConnectedServerConnections.poll(), proxy);
                                }
                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                        }).start();

                    } catch (IOException e1) {
                    }
                }



            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void abort() {
        stateSetter.setState(HState.ABORTING);
        if (hasMapped) {
            ipMapper.deleteMapping(proxy.getActual_domain());
            hasMapped = false;
        }
        tryCloseProxy();
        stateSetter.setState(HState.NOT_CONNECTED);
    }

    @Override
    protected void onConnectEnd() {
        if (hasMapped) {
            ipMapper.deleteMapping(proxy.getActual_domain());
            hasMapped = false;
        }
        tryCloseProxy();
        super.onConnectEnd();
    }

    private void tryCloseProxy() {
        if (proxy.getProxy_server() != null && !proxy.getProxy_server().isClosed())	{
            try {
                proxy.getProxy_server().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
