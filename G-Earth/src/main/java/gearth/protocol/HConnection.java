package gearth.protocol;

import gearth.misc.Cacher;
import gearth.misc.OSValidator;
import gearth.protocol.hostreplacer.HostReplacer;
import gearth.protocol.hostreplacer.HostReplacerFactory;
import gearth.protocol.memory.Rc4Obtainer;
import gearth.protocol.packethandler.Handler;
import gearth.protocol.packethandler.IncomingHandler;
import gearth.protocol.packethandler.OutgoingHandler;
import org.json.JSONArray;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class HConnection {

    public static final String HOTELS_CACHE_KEY = "hotelsConnectionInfo";

    private final Queue<HPacket> sendToClientAsyncQueue = new LinkedList<>();
    private final Queue<HPacket> sendToServerAsyncQueue = new LinkedList<>();
    public HConnection() {
        new Thread(() -> {
            while (true) {
                HPacket packet;
                synchronized (sendToClientAsyncQueue) {
                    while ((packet = sendToClientAsyncQueue.poll()) != null) {
                        sendToClient(packet);
                    }
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) { //java........................................
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                HPacket packet;
                synchronized (sendToServerAsyncQueue) {
                    while ((packet = sendToServerAsyncQueue.poll()) != null) {
                        sendToServer(packet);
                    }
                }

                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public enum State {
        NOT_CONNECTED,
        PREPARING,          // DOMAIN AND PORT BEEN PASSED
        PREPARED,           // FOUND IP ADDRESS OF DOMAIN
        WAITING_FOR_CLIENT, // WAITING FOR CORRECT TCP CONNECTION TO BE SET UP
        CONNECTED           // CONNECTED
    }

    public static List<String> autoDetectHosts;
    static {
        autoDetectHosts = new ArrayList<>();
        autoDetectHosts.add("game-br.habbo.com:30000");
        autoDetectHosts.add("game-de.habbo.com:30000");
        autoDetectHosts.add("game-es.habbo.com:30000");
        autoDetectHosts.add("game-fi.habbo.com:30000");
        autoDetectHosts.add("game-fr.habbo.com:30000");
        autoDetectHosts.add("game-it.habbo.com:30000");
        autoDetectHosts.add("game-nl.habbo.com:30000");
        autoDetectHosts.add("game-tr.habbo.com:30000");
        autoDetectHosts.add("game-us.habbo.com:38101");

        List<Object> additionalCachedHotels = Cacher.getList(HOTELS_CACHE_KEY);
        if (additionalCachedHotels != null) {
            for (Object additionalHotel : additionalCachedHotels) {
                if (!autoDetectHosts.contains(additionalHotel)) {
                    autoDetectHosts.add((String)additionalHotel);
                }
            }
        }

        if (OSValidator.isMac()) {
            for (int i = 2; i <= autoDetectHosts.size(); i++) {
                ProcessBuilder allowLocalHost = new ProcessBuilder("ifconfig", "lo0", "alias", ("127.0.0." + i), "up");
                try {
                    allowLocalHost.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public final static boolean DEBUG = false;
    private static final HostReplacer hostsReplacer = HostReplacerFactory.get();

    private volatile boolean hostRedirected = false;
    private volatile Object[] trafficListeners = {new ArrayList<TrafficListener>(), new ArrayList<TrafficListener>(), new ArrayList<TrafficListener>()};
    private volatile List<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private volatile State state = State.NOT_CONNECTED;

    private class Proxy {
        private volatile String input_domain;           //string representation of the domain to intercept
        private volatile String actual_domain;          //dns resolved domain (ignoring hosts file)
        private volatile int actual_port;               //port of the server

        private volatile int intercept_port;            //port used to intercept connection (with the current implementation, must equal actual_port)
        private volatile String intercept_host;         //local ip used to intercept host, example 127.0.0.1

        private volatile ServerSocket proxy_server = null;     //listener for the client

        private volatile IncomingHandler inHandler = null;     //connection with client (only initialized when verified habbo connection)
        private volatile OutgoingHandler outHandler = null;    //connection with server (only initialized when verified habbo connection)


        public Proxy(String input_domain, String actual_domain, int actual_port, int intercept_port, String intercept_host) {
            this.input_domain = input_domain;
            this.actual_domain = actual_domain;
            this.actual_port = actual_port;
            this.intercept_host = intercept_host;
            this.intercept_port = intercept_port;
        }

        public void initProxy(ServerSocket socket) {
            this.proxy_server = socket;
        }

        public void verifyProxy(IncomingHandler incomingHandler, OutgoingHandler outgoingHandler) {
            this.inHandler = incomingHandler;
            this.outHandler = outgoingHandler;
        }

        public int getActual_port() {
            return actual_port;
        }

        public int getIntercept_port() {
            return intercept_port;
        }

        public ServerSocket getProxy_server() {
            return proxy_server;
        }

        public String getActual_domain() {
            return actual_domain;
        }

        public String getInput_domain() {
            return input_domain;
        }

        public String getIntercept_host() {
            return intercept_host;
        }

        public IncomingHandler getInHandler() {
            return inHandler;
        }

        public OutgoingHandler getOutHandler() {
            return outHandler;
        }
    }

    private volatile List<Proxy> potentialProxies = new ArrayList<>();
    private volatile Proxy actual_proxy = null;
    private volatile String hotelVersion = "";


    public State getState() {
        return state;
    }

    // autodetect method
    public void prepare() {
        prepare(autoDetectHosts);
    }

    // manual method
    public void prepare(String domain, int port) {
        List<Object> additionalCachedHotels = Cacher.getList(HOTELS_CACHE_KEY);
        if (additionalCachedHotels == null) {
            additionalCachedHotels = new ArrayList<>();
        }
        if (!additionalCachedHotels.contains(domain +":"+port)) {
            additionalCachedHotels.add(domain+":"+port);
            Cacher.put(HOTELS_CACHE_KEY, additionalCachedHotels);
        }

        List<String> potentialHost = new ArrayList<>();
        potentialHost.add(domain+":"+port);
        prepare(potentialHost);
        actual_proxy = null;
    }

    private void prepare(List<String> allPotentialHosts) {
        setState(State.PREPARING);
        clearAllProxies();
        actual_proxy = null;

        if (hostRedirected)	{
            removeFromHosts();
        }

        List<String> willremove = new ArrayList<>();

        int c = 0;

        for (String host : allPotentialHosts) {
            String[] split = host.split(":");
            String input_dom = split[0];
            int port = Integer.parseInt(split[1]);
            String actual_dom;

            InetAddress address = null;
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
            potentialProxies.add(new Proxy(input_dom, actual_dom, port, intercept_port, intercept_host));
            c++;
        }


        List<Object> additionalCachedHotels = Cacher.getList(HOTELS_CACHE_KEY);
        if (additionalCachedHotels != null) {
            for (String host : willremove) {
                additionalCachedHotels.remove(host);
            }
            Cacher.put(HOTELS_CACHE_KEY, additionalCachedHotels);
        }

        setState(State.PREPARED);
    }

    public void start() throws IOException	{
        if (state == State.PREPARED)	{

            setState(State.WAITING_FOR_CLIENT);
            if (!hostRedirected)	{
                addToHosts();
            }


            for (int c = 0; c < potentialProxies.size(); c++) {
                Proxy potentialProxy = potentialProxies.get(c);

                ServerSocket proxy_server = new ServerSocket(potentialProxy.getIntercept_port(), 10, InetAddress.getByName(potentialProxy.getIntercept_host()));
                potentialProxy.initProxy(proxy_server);

                new Thread(() -> {
                    try  {
                        Thread.sleep(100);
                        while ((state == State.WAITING_FOR_CLIENT) && !proxy_server.isClosed())	{
                            try {
                                Socket client = proxy_server.accept();
                                client.setTcpNoDelay(true);
                                actual_proxy = potentialProxy;
                                closeAllProxies(actual_proxy);
                                if (DEBUG) System.out.println("accepted a proxy");

                                new Thread(() -> {
                                    try {
                                        startProxyThread(client, potentialProxy);
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


            if (DEBUG) System.out.println("done waiting for clients with: " + this.state );
        }
    }
    private void startProxyThread(Socket client, Proxy proxy) throws InterruptedException, UnknownHostException, IOException	{
        final boolean[] datastream = new boolean[1];

        Socket habbo_server = new Socket(proxy.actual_domain, proxy.actual_port);
        habbo_server.setTcpNoDelay(true);

        OutputStream client_out = client.getOutputStream();
        InputStream client_in = client.getInputStream();
        OutputStream habbo_server_out = habbo_server.getOutputStream();
        InputStream habbo_server_in = habbo_server.getInputStream();

        if (DEBUG) System.out.println(habbo_server.getLocalAddress().getHostAddress() + ": " + habbo_server.getLocalPort());

        final boolean[] aborted = new boolean[1];
        Rc4Obtainer rc4Obtainer = new Rc4Obtainer(this);

        OutgoingHandler outgoingHandler = new OutgoingHandler(habbo_server_out, trafficListeners);
        rc4Obtainer.setOutgoingHandler(outgoingHandler);

        IncomingHandler incomingHandler = new IncomingHandler(client_out, trafficListeners);
        rc4Obtainer.setIncomingHandler(incomingHandler);

        outgoingHandler.addOnDatastreamConfirmedListener(hotelVersion -> {
            incomingHandler.setAsDataStream();
            this.hotelVersion = hotelVersion;
            onConnect();

            setState(State.CONNECTED);
            proxy.verifyProxy(incomingHandler, outgoingHandler);
        });

        // wachten op data van client
        new Thread(() -> {
            try {
                while (!client.isClosed() && (state == State.WAITING_FOR_CLIENT || state == State.CONNECTED)) {
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
                if (DEBUG) System.out.println("abortclient");
                try {
                    if (habbo_server_out != null) habbo_server_out.close();
                    if (habbo_server_in != null) habbo_server_in.close();
                    if (client_in != null) client_in.close();
                    if (client_out != null) client_out.close();
                    if (habbo_server != null && !habbo_server.isClosed()) habbo_server.close();
                    if (client != null && !client.isClosed()) client.close();
                    aborted[0] = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (datastream[0]) {
                    setState(State.NOT_CONNECTED);
                    proxy.verifyProxy(null, null);
                    actual_proxy = null;
                };
            }
        }).start();
        // wachten op data van server
        new Thread(() -> {
            try {
                while (!habbo_server.isClosed() && (state == State.CONNECTED || state == State.WAITING_FOR_CLIENT)) {
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
                    if (!habbo_server.isClosed()) habbo_server.close();
                    if (!client.isClosed()) client.close();
                    aborted[0] = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        while(!aborted[0])
        {
            Thread.sleep(50);
        }

        try	{
            if (!habbo_server.isClosed()) habbo_server.close();
            if (!client.isClosed()) client.close();
            if (DEBUG) System.out.println("STOP");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void onConnect()	{
        if (hostRedirected)	{
            removeFromHosts();
        }

        clearAllProxies();
    }
    public void abort()	{
        if (hostRedirected)	{
            removeFromHosts();
        }

        actual_proxy = null;

        setState(State.NOT_CONNECTED);
        clearAllProxies();
    }

    private void clearAllProxies() {
        closeAllProxies(null);
        potentialProxies = new ArrayList<>();
    }
    private void closeAllProxies(Proxy except) {
        for (Proxy proxy : potentialProxies) {
            if (except != proxy) {
                if (proxy.proxy_server != null && !proxy.proxy_server.isClosed())	{
                    try {
                        proxy.proxy_server.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private void addToHosts() {
        List<String> linesTemp = new ArrayList<>();
        for (Proxy proxy : potentialProxies) {
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
        for (Proxy proxy : potentialProxies) {
            linesTemp.add(proxy.getIntercept_host() + " " + proxy.getInput_domain());
        }

        String[] lines = new String[linesTemp.size()];
        for (int i = 0; i < linesTemp.size(); i++) {
            lines[i] = linesTemp.get(i);
        }
        hostsReplacer.removeRedirect(lines);
        hostRedirected = false;
    }

    private void setState(State state) {
        if (state == State.CONNECTED) {
            sendToClientAsyncQueue.clear();
            sendToServerAsyncQueue.clear();
        }
        if (state != this.state) {
            if (state != State.CONNECTED) {
                hotelVersion = "";
            }

            State buffer = this.state;
            this.state = state;
            for (StateChangeListener listener : stateChangeListeners) {
                listener.stateChanged(buffer, state);
            }
        }

    }

    /**
     * 3 orders:
     * 0 = before modification  ¹
     * 1 = modification
     * 2 = after modification   ¹
     *
     * ¹don't edit the packet (block, replace)
     */
    public void addTrafficListener(int order, TrafficListener listener) {
        ((List<TrafficListener>)trafficListeners[order]).add(listener);
    }
    public void removeTrafficListener(TrafficListener listener) {
        ((List<TrafficListener>)trafficListeners[0]).remove(listener);
        ((List<TrafficListener>)trafficListeners[1]).remove(listener);
        ((List<TrafficListener>)trafficListeners[2]).remove(listener);
    }

    public void addStateChangeListener(StateChangeListener listener) {
        stateChangeListeners.add(listener);
    }
    public void removeStateChangeListener(StateChangeListener listener) {
        stateChangeListeners.remove(listener);
    }

    public int getPort() {
        if (actual_proxy == null) return -1;
        return actual_proxy.getIntercept_port();
    }
    public String getHost() {
        if (actual_proxy == null) return "<auto-detect>";
        return actual_proxy.getActual_domain();
    }
    public String getDomain() {
        if (actual_proxy == null) return "<auto-detect>";
        return actual_proxy.getInput_domain();
    }


    private boolean sendToClient(HPacket message) {
        if (actual_proxy == null || actual_proxy.getInHandler() == null) return false;
        actual_proxy.getInHandler().sendToStream(message.toBytes());
        return true;
    }
    private boolean sendToServer(HPacket message) {
        if (actual_proxy == null || actual_proxy.getOutHandler() == null)  return false;
        actual_proxy.getOutHandler().sendToStream(message.toBytes());
        return true;
    }

    public void sendToClientAsync(HPacket message) {
        synchronized (sendToClientAsyncQueue) {
            sendToClientAsyncQueue.add(message);
        }

    }
    public void sendToServerAsync(HPacket message) {
        synchronized (sendToServerAsyncQueue) {
            sendToServerAsyncQueue.add(message);
        }
    }

    public String getClientHostAndPort() {
        if (actual_proxy == null || actual_proxy.getIntercept_host() == null || actual_proxy.getIntercept_port() == -1) {
            return "";
        }
        return actual_proxy.getIntercept_host() + ":" + actual_proxy.getIntercept_port();
    }

    public String getHotelVersion() {
        return hotelVersion;
    }
}
