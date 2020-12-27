package gearth.protocol.connection.proxy.flash.unix;

import gearth.protocol.HConnection;
import gearth.protocol.connection.*;
import gearth.protocol.connection.proxy.flash.FlashProxyProvider;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.SocksConfiguration;
import gearth.protocol.hostreplacer.ipmapping.IpMapper;
import gearth.protocol.hostreplacer.ipmapping.IpMapperFactory;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

public class LinuxRawIpFlashProxyProvider extends FlashProxyProvider {

    private volatile String input_host;
    private volatile int input_port;

    protected IpMapper ipMapper = IpMapperFactory.get();
    protected HProxy proxy = null;

    private boolean useSocks;

    public LinuxRawIpFlashProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, String input_host, int input_port, boolean useSocks) {
        super(proxySetter, stateSetter, hConnection);
        this.input_host = input_host;
        this.input_port = input_port;
        this.useSocks = useSocks;
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
                proxy = new HProxy(HClient.FLASH, input_host, input_host, input_port, input_port, "0.0.0.0");

                maybeRemoveMapping();

                if (!onBeforeIpMapping()) {
                    stateSetter.setState(HState.NOT_CONNECTED);
                    return;
                }

                maybeAddMapping();

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
                                createProxyThread(client);
                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                        }).start();

                    } catch (IOException ignored) {
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
        maybeRemoveMapping();
        tryCloseProxy();
        super.abort();
    }

    @Override
    protected void onConnect() {
        super.onConnect();
        maybeRemoveMapping();
        tryCloseProxy();
    }

    @Override
    protected void onConnectEnd() {
        maybeRemoveMapping();
        tryCloseProxy();
        super.onConnectEnd();
    }

    protected void tryCloseProxy() {
        if (proxy.getProxy_server() != null && !proxy.getProxy_server().isClosed())	{
            try {
                proxy.getProxy_server().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Queue<Socket> preConnectedServerConnections;

    // returns false if fail
    protected boolean onBeforeIpMapping() throws IOException, InterruptedException {
        if (useSocks) {
            return true;
        }

        preConnectedServerConnections = new LinkedList<>();
        for (int i = 0; i < 3; i++) {
            Socket s1 = new Socket();
            s1.setSoTimeout(1200);
            try {
                s1.connect(new InetSocketAddress(proxy.getActual_domain(), proxy.getActual_port()), 1200);
            }
            catch (SocketTimeoutException e) {
                showInvalidConnectionError();
                return false;
            }

            preConnectedServerConnections.add(s1);
            Thread.sleep(50);
        }

        return true;
    }

    protected void createProxyThread(Socket client) throws IOException, InterruptedException {
        if (useSocks) {
            createSocksProxyThread(client);
        }
        else if (preConnectedServerConnections.isEmpty()) {
            if (HConnection.DEBUG) System.out.println("pre-made server connections ran out of stock");
        }
        else {
            startProxyThread(client, preConnectedServerConnections.poll(), proxy);
        }
    }

    private void createSocksProxyThread(Socket client) throws SocketException {
        SocksConfiguration configuration = ProxyProviderFactory.getSocksConfig();

        if (configuration == null) {
            maybeRemoveMapping();
            stateSetter.setState(HState.NOT_CONNECTED);
            showInvalidConnectionError();
            return;
        }

        Socket server = configuration.createSocket();
        try {
            server.connect(new InetSocketAddress(proxy.getActual_domain(), proxy.getActual_port()), 5000);
            startProxyThread(client, server, proxy);
        }
        catch (Exception e) {
            maybeRemoveMapping();
            stateSetter.setState(HState.NOT_CONNECTED);
            showInvalidConnectionError();
            e.printStackTrace();
        }
    }

    protected void maybeAddMapping() {
        ipMapper.enable();
        ipMapper.addMapping(proxy.getActual_domain(), proxy.getActual_port(), proxy.getIntercept_port());
    }

    protected void maybeRemoveMapping() {
        ipMapper.deleteMapping(proxy.getActual_domain(), proxy.getActual_port(), proxy.getIntercept_port());

    }

}
