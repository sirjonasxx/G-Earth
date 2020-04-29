package gearth.protocol;

import gearth.misc.Cacher;
import gearth.misc.OSValidator;
import gearth.misc.listenerpattern.Observable;
import gearth.protocol.connection.HState;
import gearth.protocol.hostreplacer.hostsfile.HostReplacer;
import gearth.protocol.hostreplacer.hostsfile.HostReplacerFactory;
import gearth.protocol.hostreplacer.ipmapping.IpMapper;
import gearth.protocol.hostreplacer.ipmapping.IpMapperFactory;
import gearth.protocol.memory.Rc4Obtainer;
import gearth.protocol.packethandler.IncomingPacketHandler;
import gearth.protocol.packethandler.OutgoingPacketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class HConnection_temp {

//    public static final String HOTELS_CACHE_KEY = "hotelsConnectionInfo";
//
//    private final Queue<HPacket> sendToClientAsyncQueue = new LinkedList<>();
//    private final Queue<HPacket> sendToServerAsyncQueue = new LinkedList<>();
//    public HConnection_temp() {
//        new Thread(() -> {
//            while (true) {
//                HPacket packet;
//                synchronized (sendToClientAsyncQueue) {
//                    while ((packet = sendToClientAsyncQueue.poll()) != null) {
//                        sendToClient(packet);
//                    }
//                }
//
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) { //java........................................
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//        new Thread(() -> {
//            while (true) {
//                HPacket packet;
//                synchronized (sendToServerAsyncQueue) {
//                    while ((packet = sendToServerAsyncQueue.poll()) != null) {
//                        sendToServer(packet);
//                    }
//                }
//
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }
//    // checks if host is a raw IP instead of a domain
//    private static boolean hostIsIpAddress(String host){
//        for (char c : host.toCharArray()) {
//            if (c != '.' && (c < '0' || c > '9')) {
//                return false;
//            }
//        }
//        return true;
//    }
//
//
//    public static List<String> autoDetectHosts;
//    static {
//        autoDetectHosts = new ArrayList<>();
//        autoDetectHosts.add("game-br.habbo.com:30000");
//        autoDetectHosts.add("game-de.habbo.com:30000");
//        autoDetectHosts.add("game-es.habbo.com:30000");
//        autoDetectHosts.add("game-fi.habbo.com:30000");
//        autoDetectHosts.add("game-fr.habbo.com:30000");
//        autoDetectHosts.add("game-it.habbo.com:30000");
//        autoDetectHosts.add("game-nl.habbo.com:30000");
//        autoDetectHosts.add("game-tr.habbo.com:30000");
//        autoDetectHosts.add("game-us.habbo.com:38101");
//
//        List<Object> additionalCachedHotels = Cacher.getList(HOTELS_CACHE_KEY);
//        if (additionalCachedHotels != null) {
//            for (Object additionalHotel : additionalCachedHotels) {
//                if (!autoDetectHosts.contains(additionalHotel)) {
//                    autoDetectHosts.add((String)additionalHotel);
//                }
//            }
//        }
//
//        if (OSValidator.isMac()) {
//            for (int i = 2; i <= autoDetectHosts.size(); i++) {
//                ProcessBuilder allowLocalHost = new ProcessBuilder("ifconfig", "lo0", "alias", ("127.0.0." + i), "up");
//                try {
//                    allowLocalHost.start();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//
//    public static volatile boolean DECRYPTPACKETS = true;
//    public static volatile boolean DEBUG = false;
//    private static final HostReplacer hostsReplacer = HostReplacerFactory.get();
//
//    private volatile boolean hostRedirected = false;
//    private volatile Object[] trafficObservables = {new Observable<TrafficListener>(), new Observable<TrafficListener>(), new Observable<TrafficListener>()};
//    private volatile Observable<StateChangeListener> stateObservable = new Observable<>();
//
//    private volatile State state = State.NOT_CONNECTED;
//
//    private volatile List<Proxy> potentialProxies = new ArrayList<>();
//    private volatile Proxy actual_proxy = null;
//    private volatile String hotelVersion = "";
//
//    private volatile boolean rawIpMode = false;
//
//
//    public HState getState() {
//        return state;
//    }
//
//    // autodetect method
//    public void prepare() {
//        prepare(autoDetectHosts);
//    }
//
//    // manual method
//    public void prepare(String domain, int port) {
//        List<Object> additionalCachedHotels = Cacher.getList(HOTELS_CACHE_KEY);
//        if (additionalCachedHotels == null) {
//            additionalCachedHotels = new ArrayList<>();
//        }
//        if (!additionalCachedHotels.contains(domain +":"+port)) {
//            additionalCachedHotels.add(domain+":"+port);
//            Cacher.put(HOTELS_CACHE_KEY, additionalCachedHotels);
//        }
//
//        List<String> potentialHost = new ArrayList<>();
//        potentialHost.add(domain+":"+port);
//
//        if (hostIsIpAddress(domain)) {
//            setState(State.PREPARING); // state will not be prepared until the server-connection is initialized
//            rawIpMode = true;
//            potentialProxies.clear();
//            potentialProxies.add(new Proxy(domain, domain, port, port, "0.0.0.0"));
//        }
//        else {
//            prepare(potentialHost);
//        }
//
//        actual_proxy = null;
//    }
//
//    public Object[] getTrafficObservables() {
//        return trafficObservables;
//    }
//
//    private void prepare(List<String> allPotentialHosts) {
//        rawIpMode = false;
//        setState(State.PREPARING);
//        clearAllProxies();
//        actual_proxy = null;
//
//        if (hostRedirected)	{
//            removeFromHosts();
//        }
//
//
//        List<String> willremove = new ArrayList<>();
//        int c = 0;
//        for (String host : allPotentialHosts) {
//            String[] split = host.split(":");
//            String input_dom = split[0];
//            if (!hostIsIpAddress(input_dom)) {
//                int port = Integer.parseInt(split[1]);
//                String actual_dom;
//
//                InetAddress address = null;
//                try {
//                    address = InetAddress.getByName(input_dom);
//                    actual_dom = address.getHostAddress();
//                }
//                catch (UnknownHostException e) {
//                    willremove.add(host);
//                    continue;
//                }
//
//                int intercept_port = port;
//                String intercept_host = "127.0." + (c / 254) + "." + (1 + c % 254);
//                potentialProxies.add(new Proxy(input_dom, actual_dom, port, intercept_port, intercept_host));
//                c++;
//            }
//        }
//
//        List<Object> additionalCachedHotels = Cacher.getList(HOTELS_CACHE_KEY);
//        if (additionalCachedHotels != null) {
//            for (String host : willremove) {
//                additionalCachedHotels.remove(host);
//            }
//            Cacher.put(HOTELS_CACHE_KEY, additionalCachedHotels);
//        }
//
//
//
//        setState(State.PREPARED);
//    }
//
//    private void startForRawIp() {
//
//        new Thread(() -> {
//            try  {
//
//                Proxy proxy = potentialProxies.get(0);
//                IpMapper ipMapper = IpMapperFactory.get();
////                ipMapper.deleteMapping(proxy.actual_domain); // just making sure -> actually, dont
//
//                Queue<Socket> preConnectedServerConnections = new LinkedList<>();
//                for (int i = 0; i < 3; i++) {
//                    preConnectedServerConnections.add(new Socket(proxy.actual_domain, proxy.actual_port));
//                    Thread.sleep(50);
//                }
//
//                ipMapper.enable();
//                ipMapper.addMapping(proxy.actual_domain);
//
//                if (DEBUG) System.out.println("Added mapping for raw IP");
//
//                ServerSocket proxy_server = new ServerSocket(proxy.getIntercept_port(), 10, InetAddress.getByName(proxy.getIntercept_host()));
//                proxy.initProxy(proxy_server);
//                if (DEBUG) System.out.println("");
//
//
//                Thread.sleep(30);
//                setState(State.WAITING_FOR_CLIENT);
//
//                while ((state == State.WAITING_FOR_CLIENT) && !proxy_server.isClosed())	{
//                    try {
//                        if (DEBUG) System.out.println("try accept proxy");
//                        Socket client = proxy_server.accept();
//                        client.setTcpNoDelay(true);
//                        actual_proxy = proxy;
//                        if (DEBUG) System.out.println("accepted a proxy");
//
//                        new Thread(() -> {
//                            try {
//                                if (preConnectedServerConnections.isEmpty()) {
//                                    if (DEBUG) System.out.println("pre-made server connections ran out of stock");
//                                }
//                                else {
////                                System.out.println("hi");
////                                SocketAddress proxyAddr = new InetSocketAddress("80.248.225.58", 31431);
////                                System.out.println("hi2");
////                                java.net.Proxy pr = new java.net.Proxy(java.net.Proxy.Type.SOCKS,  proxyAddr);
////                                System.out.println("hi3");
////                                Socket server = new Socket(pr);
////                                System.out.println("hi4");
////                                server.connect(new InetSocketAddress(proxy.actual_domain, proxy.actual_port));
////                                System.out.println("hi5");
//                                    startProxyThread(client, preConnectedServerConnections.poll(), actual_proxy);
////                                startProxyThread(client, server, actual_proxy);
//                                }
//                            } catch (InterruptedException | IOException e) {
//                                e.printStackTrace();
//                            }
//                        }).start();
//
//                    } catch (IOException e1) {
//                    }
//                }
//
////                if (proxy_server.isClosed()) {
////                    if (rawIpMode) {
////                        IpMapperFactory.get().deleteMapping(proxy.actual_domain);
////                    }
////                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }).start();
//    }
//
//    public void start() throws IOException	{
//        if (state == State.PREPARING && rawIpMode) {
//            startForRawIp();
//            return;
//        }
//
//        if (state == State.PREPARED && !rawIpMode)	{
//
//            setState(State.WAITING_FOR_CLIENT);
//
//            if (!hostRedirected)	{
//                addToHosts();
//            }
//
//            for (int c = 0; c < potentialProxies.size(); c++) {
//                Proxy potentialProxy = potentialProxies.get(c);
//
//                ServerSocket proxy_server = new ServerSocket(potentialProxy.getIntercept_port(), 10, InetAddress.getByName(potentialProxy.getIntercept_host()));
//                potentialProxy.initProxy(proxy_server);
//
//                new Thread(() -> {
//                    try  {
//                        Thread.sleep(30);
//                        while ((state == State.WAITING_FOR_CLIENT) && !proxy_server.isClosed())	{
//                            try {
//                                Socket client = proxy_server.accept();
//                                client.setTcpNoDelay(true);
//                                actual_proxy = potentialProxy;
//                                closeAllProxies(actual_proxy);
//                                if (DEBUG) System.out.println("accepted a proxy");
//
//                                new Thread(() -> {
//                                    try {
//                                        Socket server = new Socket(actual_proxy.actual_domain, actual_proxy.actual_port);
//                                        startProxyThread(client, server, actual_proxy);
//                                    } catch (InterruptedException | IOException e) {
//                                        // TODO Auto-generated catch block
//                                        e.printStackTrace();
//                                    }
//                                }).start();
//
//
//                            } catch (IOException e1) {
//                                // TODO Auto-generated catch block
////                                e1.printStackTrace();
//                            }
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }).start();
//            }
//
//
//            if (DEBUG) System.out.println("done waiting for clients with: " + this.state );
//        }
//    }
//    private void startProxyThread(Socket client, Socket server, Proxy proxy) throws InterruptedException, UnknownHostException, IOException	{
//        final boolean[] datastream = new boolean[1];
//        server.setTcpNoDelay(true);
//
//        OutputStream client_out = client.getOutputStream();
//        InputStream client_in = client.getInputStream();
//        OutputStream habbo_server_out = server.getOutputStream();
//        InputStream habbo_server_in = server.getInputStream();
//
//        if (DEBUG) System.out.println(server.getLocalAddress().getHostAddress() + ": " + server.getLocalPort());
//
//        final boolean[] aborted = new boolean[1];
//        Rc4Obtainer rc4Obtainer = new Rc4Obtainer(this);
//
//        OutgoingPacketHandler outgoingHandler = new OutgoingPacketHandler(habbo_server_out, trafficObservables);
//        IncomingPacketHandler incomingHandler = new IncomingPacketHandler(client_out, trafficObservables);
//        rc4Obtainer.setPacketHandlers(outgoingHandler, incomingHandler);
//
//        outgoingHandler.addOnDatastreamConfirmedListener(hotelVersion -> {
//            incomingHandler.setAsDataStream();
//            this.hotelVersion = hotelVersion;
//            onConnect();
//
//            setState(State.CONNECTED);
//            proxy.verifyProxy(incomingHandler, outgoingHandler);
//        });
//
//        // wachten op data van client
//        new Thread(() -> {
//            try {
//                while (!client.isClosed() && (state == State.WAITING_FOR_CLIENT || state == State.CONNECTED)) {
//                    byte[] buffer;
//                    while (client_in.available() > 0)	{
//                        client_in.read(buffer = new byte[client_in.available()]);
//                        outgoingHandler.act(buffer);
//                    }
//                    Thread.sleep(1);
//
//                }
//            }
//            catch (IOException | InterruptedException e) {
//                e.printStackTrace();
//            }
//            finally {
//                if (DEBUG) System.out.println("abortclient");
//                try {
//                    if (habbo_server_out != null) habbo_server_out.close();
//                    if (habbo_server_in != null) habbo_server_in.close();
//                    if (client_in != null) client_in.close();
//                    if (client_out != null) client_out.close();
//                    if (server != null && !server.isClosed()) server.close();
//                    if (client != null && !client.isClosed()) client.close();
//                    aborted[0] = true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                if (datastream[0]) {
//                    setState(State.NOT_CONNECTED);
//                    proxy.verifyProxy(null, null);
//                    if (rawIpMode) {
//                        IpMapperFactory.get().deleteMapping(proxy.actual_domain);
//                    }
//                    actual_proxy = null;
//                };
//            }
//        }).start();
//        // wachten op data van server
//        new Thread(() -> {
//            try {
//                while (!server.isClosed() && (state == State.CONNECTED || state == State.WAITING_FOR_CLIENT)) {
//                    byte[] buffer;
//                    while (habbo_server_in.available() > 0) {
//                        habbo_server_in.read(buffer = new byte[habbo_server_in.available()]);
//                        incomingHandler.act(buffer);
//                    }
//                    Thread.sleep(1);
//                }
//            } catch (IOException | InterruptedException e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    if (habbo_server_out != null) habbo_server_out.close();
//                    if (habbo_server_in != null) habbo_server_in.close();
//                    if (client_in != null) client_in.close();
//                    if (client_out != null) client_out.close();
//                    if (!server.isClosed()) server.close();
//                    if (!client.isClosed()) client.close();
//                    aborted[0] = true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//
//        while(!aborted[0]) {
//            Thread.sleep(50);
//        }
//
//        try	{
//            if (!server.isClosed()) server.close();
//            if (!client.isClosed()) client.close();
//            if (DEBUG) System.out.println("STOP");
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//
//    private void onConnect()	{
//        if (hostRedirected)	{
//            removeFromHosts();
//        }
//
//        clearAllProxies();
//
//    }
//    public void abort()	{
//        setState(State.ABORTING);
//        if (hostRedirected)	{
//            removeFromHosts();
//        }
//
//        if (rawIpMode && potentialProxies.size() == 1) {
//            IpMapperFactory.get().deleteMapping(potentialProxies.get(0).actual_domain);
//        }
//        if (rawIpMode && actual_proxy != null) {
//            IpMapperFactory.get().deleteMapping(actual_proxy.actual_domain);
//        }
//
//        actual_proxy = null;
//
//        clearAllProxies();
//        setState(State.NOT_CONNECTED);
//    }
//
//    private void clearAllProxies() {
//        closeAllProxies(null);
//        potentialProxies = new ArrayList<>();
//    }
//    private void closeAllProxies(Proxy except) {
//        for (Proxy proxy : potentialProxies) {
//            if (except != proxy) {
//                if (proxy.getProxy_server() != null && !proxy.getProxy_server().isClosed())	{
//                    try {
//                        proxy.getProxy_server().close();
//                    } catch (IOException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//        }
//    }
//
//    private void addToHosts() {
//        List<String> linesTemp = new ArrayList<>();
//        for (Proxy proxy : potentialProxies) {
//            linesTemp.add(proxy.getIntercept_host() + " " + proxy.getInput_domain());
//        }
//
//        String[] lines = new String[linesTemp.size()];
//        for (int i = 0; i < linesTemp.size(); i++) {
//            lines[i] = linesTemp.get(i);
//        }
//        hostsReplacer.addRedirect(lines);
//        hostRedirected = true;
//    }
//    private void removeFromHosts(){
//        List<String> linesTemp = new ArrayList<>();
//        for (Proxy proxy : potentialProxies) {
//            linesTemp.add(proxy.getIntercept_host() + " " + proxy.getInput_domain());
//        }
//
//        String[] lines = new String[linesTemp.size()];
//        for (int i = 0; i < linesTemp.size(); i++) {
//            lines[i] = linesTemp.get(i);
//        }
//        hostsReplacer.removeRedirect(lines);
//        hostRedirected = false;
//    }
//
//    private void setState(State state) {
//        if (state == State.CONNECTED) {
//            sendToClientAsyncQueue.clear();
//            sendToServerAsyncQueue.clear();
//        }
//        if (state != this.state) {
//            if (state != State.CONNECTED) {
//                hotelVersion = "";
//            }
//
//            State buffer = this.state;
//            this.state = state;
//            stateObservable.fireEvent(l -> l.stateChanged(buffer, state));
//        }
//
//    }
//
//    public Observable<StateChangeListener> getStateObservable() {
//        return stateObservable;
//    }
//
//    /**
//     * 3 orders:
//     * 0 = before modification  ¹
//     * 1 = modification
//     * 2 = after modification   ¹
//     *
//     * ¹don't edit the packet (block, replace)
//     */
//    public void addTrafficListener(int order, TrafficListener listener) {
//        ((Observable<TrafficListener>) trafficObservables[order]).addListener(listener);
//    }
//    public void removeTrafficListener(TrafficListener listener) {
//        ((Observable<TrafficListener>) trafficObservables[0]).removeListener(listener);
//        ((Observable<TrafficListener>) trafficObservables[1]).removeListener(listener);
//        ((Observable<TrafficListener>) trafficObservables[2]).removeListener(listener);
//    }
//
//    public int getServerPort() {
//        if (actual_proxy == null) return -1;
//        return actual_proxy.getIntercept_port();
//    }
//    public String getServerHost() {
//        if (actual_proxy == null) return "<auto-detect>";
//        return actual_proxy.getActual_domain();
//    }
//    public String getDomain() {
//        if (actual_proxy == null) return "<auto-detect>";
//        return actual_proxy.getInput_domain();
//    }
//
//
//    private boolean sendToClient(HPacket message) {
//        if (actual_proxy == null || actual_proxy.getInHandler() == null) return false;
//        actual_proxy.getInHandler().sendToStream(message.toBytes());
//        return true;
//    }
//    private boolean sendToServer(HPacket message) {
//        if (actual_proxy == null || actual_proxy.getOutHandler() == null)  return false;
//        actual_proxy.getOutHandler().sendToStream(message.toBytes());
//        return true;
//    }
//
//    public void sendToClientAsync(HPacket message) {
//        synchronized (sendToClientAsyncQueue) {
//            sendToClientAsyncQueue.add(message);
//        }
//
//    }
//    public void sendToServerAsync(HPacket message) {
//        synchronized (sendToServerAsyncQueue) {
//            sendToServerAsyncQueue.add(message);
//        }
//    }
//
//    public String getClientHost() {
//        if (actual_proxy == null) {
//            return "";
//        }
//        return actual_proxy.getIntercept_host();
//    }
//
//    public int getClientPort() {
//        if (actual_proxy == null) {
//            return -1;
//        }
//        return actual_proxy.getIntercept_port();
//    }
//
//    public String getHotelVersion() {
//        return hotelVersion;
//    }
//
//
//    public boolean isRawIpMode() {
//        return rawIpMode;
//    }
}
