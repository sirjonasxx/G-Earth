package gearth.protocol;

import gearth.misc.Cacher;
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
    }


    public final static boolean DEBUG = false;
    private static final HostReplacer hostsReplacer = HostReplacerFactory.get();

    private volatile boolean hostRedirected = false;
    private volatile Object[] trafficListeners = {new ArrayList<TrafficListener>(), new ArrayList<TrafficListener>(), new ArrayList<TrafficListener>()};
    private volatile List<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private volatile State state = State.NOT_CONNECTED;

    private volatile List<String> input_domain = new ArrayList<>();        // given string representation
    private volatile List<String> actual_domain = new ArrayList<>();              // actual ip representation
    private volatile List<Integer> port = new ArrayList<>();

    private volatile List<ServerSocket> proxy = new ArrayList<>();
    private volatile int realProxyIndex = -1;

    private volatile Handler inHandler = null;
    private volatile Handler outHandler = null;

    private volatile boolean autoDetectHost = false;

    private volatile String clientHostAndPort = "";
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
        realProxyIndex = 0;
    }

    private void prepare(List<String> allPotentialHosts) {
        setState(State.PREPARING);
        input_domain.clear();
        actual_domain.clear();
        port.clear();
        clearAllProxies();
        realProxyIndex = -1;

        for (String host : allPotentialHosts) {
            String[] split = host.split(":");
            input_domain.add(split[0]);
            port.add(Integer.parseInt(split[1]));
        }

        if (hostRedirected)	{
            removeFromHosts();
        }


        List<String> willremove = new ArrayList<>();
        for (String host : allPotentialHosts) {
            InetAddress address = null;
            try {
                address = InetAddress.getByName(host.split(":")[0]);
                actual_domain.add(address.getHostAddress());
            } catch (UnknownHostException e) {
//                e.printStackTrace();
                actual_domain.add(null);
                willremove.add(host);
            }
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


            for (int i = 0; i < actual_domain.size(); i++) {
                if (actual_domain.get(i) == null) continue;

                ServerSocket proxy = new ServerSocket(port.get(i), 10, InetAddress.getByName("127.0.0." + (i+1)));
                this.proxy.add(proxy);
                String dom = actual_domain.get(i);
                Integer port2 = port.get(i);

                int[] i2 = {i};

                new Thread(() -> {
                    try  {
                        Thread.sleep(100);
                        while ((state == State.WAITING_FOR_CLIENT) && !proxy.isClosed())	{
                            try {
                                Socket client = proxy.accept();
                                realProxyIndex = i2[0];
                                closeAllProxies(i2[0]);
                                if (DEBUG) System.out.println("accepted a proxy");

                                new Thread(() -> {
                                    try {
                                        startProxyThread(client, dom, port2);
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
    private void startProxyThread(Socket client, String ip, int port) throws InterruptedException, UnknownHostException, IOException	{
        final boolean[] datastream = new boolean[1];

        Socket habbo_server = new Socket(ip, port);

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
            this.hotelVersion = hotelVersion;
            incomingHandler.setAsDataStream();
            clientHostAndPort = client.getLocalAddress().getHostAddress() + ":" + client.getPort();
            if (DEBUG) System.out.println(clientHostAndPort);
            setState(State.CONNECTED);
            onConnect();
            outHandler = outgoingHandler;
            inHandler = incomingHandler;
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
                    outHandler = null;
                    inHandler = null;
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

        port.clear();
        input_domain.clear();
        actual_domain.clear();
        realProxyIndex = -1;

        setState(State.NOT_CONNECTED);
        clearAllProxies();
    }

    private void clearAllProxies() {
        closeAllProxies(-1);
        proxy.clear();
    }
    private void closeAllProxies(int except) {
        for (int i = 0; i < proxy.size(); i++) {
            if (i != except) {
                ServerSocket prox = proxy.get(i);
                if (prox != null && !prox.isClosed())	{
                    try {
                        prox.close();
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
        for (int i = 0; i < input_domain.size(); i++) {
            if (actual_domain.get(i) != null) {
                linesTemp.add(("127.0.0." + (i+1)) + " " + input_domain.get(i));
            }
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
        for (int i = 0; i < input_domain.size(); i++) {
            if (actual_domain.get(i) != null) {
                linesTemp.add(("127.0.0." + (i+1)) + " " + input_domain.get(i));
            }
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
                clientHostAndPort = "";
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
        if (realProxyIndex == -1) return -1;
        return port.get(realProxyIndex);
    }
    public String getHost() {
        if (realProxyIndex == -1) return "<auto-detect>";
        return actual_domain.get(realProxyIndex);
    }
    public String getDomain() {
        if (realProxyIndex == -1) return "<auto-detect>";
        return input_domain.get(realProxyIndex);
    }


    public boolean sendToClient(HPacket message) {
        if (inHandler == null) return false;
        inHandler.sendToStream(message.toBytes());
        return true;
    }
    public boolean sendToServer(HPacket message) {
        if (outHandler == null) return false;
        outHandler.sendToStream(message.toBytes());
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
        return clientHostAndPort;
    }

    public String getHotelVersion() {
        return hotelVersion;
    }
}
