package gearth.protocol.connection.proxy;

import gearth.misc.Cacher;
import gearth.protocol.HConnection;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HProxySetter;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.HStateSetter;
import gearth.protocol.hostreplacer.hostsfile.HostReplacer;
import gearth.protocol.hostreplacer.hostsfile.HostReplacerFactory;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NormalProxyProvider extends ProxyProvider {

    private List<String> potentialHosts;


    private static final HostReplacer hostsReplacer = HostReplacerFactory.get();
    private volatile boolean hostRedirected = false;

    private volatile List<HProxy> potentialProxies = new ArrayList<>();
    private volatile HProxy proxy = null;



    public NormalProxyProvider(HProxySetter proxySetter, HStateSetter stateSetter, HConnection hConnection, List<String> potentialHosts) {
        super(proxySetter, stateSetter, hConnection);
        this.potentialHosts = potentialHosts;
    }


    @Override
    public void start() throws IOException {
        if (hConnection.getState() != HState.NOT_CONNECTED) {
            return;
        }

        prepare();
        addToHosts();
        launchProxy();

    }

    private void prepare() {
        stateSetter.setState(HState.PREPARING);

        List<String> willremove = new ArrayList<>();
        int c = 0;
        for (String host : potentialHosts) {
            String[] split = host.split(":");
            String input_dom = split[0];
            if (!ProxyProviderFactory.hostIsIpAddress(input_dom)) {
                int port = Integer.parseInt(split[1]);
                String actual_dom;

                InetAddress address;
                try {
                    address = InetAddress.getByName(input_dom);
                    actual_dom = address.getHostAddress();
                }
                catch (UnknownHostException e) {
                    willremove.add(host);
                    continue;
                }

                int intercept_port = port;
                String intercept_host = "127.0." + (c / 254) + "." + (1 + c % 254);
                potentialProxies.add(new HProxy(input_dom, actual_dom, port, intercept_port, intercept_host));
                c++;
            }
        }

        List<Object> additionalCachedHotels = Cacher.getList(ProxyProviderFactory.HOTELS_CACHE_KEY);
        if (additionalCachedHotels != null) {
            for (String host : willremove) {
                additionalCachedHotels.remove(host);
            }
            Cacher.put(ProxyProviderFactory.HOTELS_CACHE_KEY, additionalCachedHotels);
        }

        stateSetter.setState(HState.PREPARED);
    }

    private void launchProxy() throws IOException {
        stateSetter.setState(HState.WAITING_FOR_CLIENT);

        for (int c = 0; c < potentialProxies.size(); c++) {
            HProxy potentialProxy = potentialProxies.get(c);

            ServerSocket proxy_server = new ServerSocket(potentialProxy.getIntercept_port(), 10, InetAddress.getByName(potentialProxy.getIntercept_host()));
            potentialProxy.initProxy(proxy_server);

            new Thread(() -> {
                try  {
                    Thread.sleep(30);
                    while ((hConnection.getState() == HState.WAITING_FOR_CLIENT) && !proxy_server.isClosed())	{
                        try {
                            Socket client = proxy_server.accept();
                            proxy = potentialProxy;
                            closeAllProxies(proxy);
                            if (HConnection.DEBUG) System.out.println("accepted a proxy");

                            new Thread(() -> {
                                try {
                                    Socket server = new Socket(proxy.getActual_domain(), proxy.getActual_port());
                                    startProxyThread(client, server, proxy);
                                } catch (InterruptedException | IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }).start();


                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
//                                e1.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }


        if (HConnection.DEBUG) System.out.println("done waiting for clients with: " + hConnection.getState() );

    }

    @Override
    public void abort() {
        stateSetter.setState(HState.ABORTING);
        if (hostRedirected)	{
            removeFromHosts();
        }

        clearAllProxies();
        stateSetter.setState(HState.NOT_CONNECTED);
    }

    @Override
    protected void onConnect() {
        super.onConnect();

        if (hostRedirected)	{
            removeFromHosts();
        }
        clearAllProxies();
    }

    private void addToHosts() {
        List<String> linesTemp = new ArrayList<>();
        for (HProxy proxy : potentialProxies) {
            linesTemp.add(proxy.getIntercept_host() + " " + proxy.getInput_domain());
        }

        String[] lines = new String[linesTemp.size()];
        for (int i = 0; i < linesTemp.size(); i++) {
            lines[i] = linesTemp.get(i);
        }
        hostsReplacer.addRedirect(lines);
        hostRedirected = true;
    }
    private void removeFromHosts(){
        List<String> linesTemp = new ArrayList<>();
        for (HProxy proxy : potentialProxies) {
            linesTemp.add(proxy.getIntercept_host() + " " + proxy.getInput_domain());
        }

        String[] lines = new String[linesTemp.size()];
        for (int i = 0; i < linesTemp.size(); i++) {
            lines[i] = linesTemp.get(i);
        }
        hostsReplacer.removeRedirect(lines);
        hostRedirected = false;
    }

    private void clearAllProxies() {
        closeAllProxies(null);
        potentialProxies = new ArrayList<>();
    }
    private void closeAllProxies(HProxy except) {
        for (HProxy proxy : potentialProxies) {
            if (except != proxy) {
                if (proxy.getProxy_server() != null && !proxy.getProxy_server().isClosed())	{
                    try {
                        proxy.getProxy_server().close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        potentialProxies = Collections.singletonList(except);
    }
}
