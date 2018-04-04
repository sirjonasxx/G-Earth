package main.protocol;

import main.OSValidator;
import main.protocol.memory.Rc4Obtainer;
import main.protocol.packethandler.Handler;
import main.protocol.packethandler.IncomingHandler;
import main.protocol.packethandler.OutgoingHandler;
import sun.plugin2.util.SystemUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class HConnection {

    public enum State {
        NOT_CONNECTED,
        PREPARING,          // DOMAIN AND PORT BEEN PASSED
        PREPARED,           // FOUND IP ADDRESS OF DOMAIN
        WAITING_FOR_CLIENT, // WAITING FOR CORRECT TCP CONNECTION TO BE SET UP
        CONNECTED           // CONNECTED
    }

    private static final String hostsFileLocation;
    static {
        if (OSValidator.isWindows()) hostsFileLocation = System.getenv("WinDir") + "\\system32\\drivers\\etc\\hosts";
        else hostsFileLocation = "/etc/hosts"; // confirmed location on linux & mac
    }

    private volatile boolean hostFileEdited = false;
    private volatile Object[] trafficListeners = {new ArrayList<TrafficListener>(), new ArrayList<TrafficListener>(), new ArrayList<TrafficListener>()};
    private volatile List<StateChangeListener> stateChangeListeners = new ArrayList<>();
    private volatile State state = State.NOT_CONNECTED;
    private volatile String input_domain = null;        // given string representation

    private volatile String actual_domain;              // actual ip representation
    private volatile int port = -1;

    private volatile ServerSocket proxy;
    private volatile Handler inHandler = null;
    private volatile Handler outHandler = null;

    public final static boolean DEBUG = false;

    public State getState() {
        return state;
    }
    private String detourIP() {
        return "127.0.0.1";
    }

    public void prepare(String domain, int port) {
        setState(State.PREPARING);

        this.actual_domain = domain;
        this.port = port;

        if (hostFileEdited)	{
            removeFromHostsFile(detourIP() + " " + domain);
        }

        try {
            InetAddress address = InetAddress.getByName(domain);
            actual_domain = address.getHostAddress();
            if (DEBUG) System.out.println("found dom:" + actual_domain);
            input_domain = domain;
            setState(State.PREPARED);

        } catch (UnknownHostException e) {
            e.printStackTrace();
            setState(State.NOT_CONNECTED);
        }
    }
    public void start() throws IOException	{
        if (state == State.PREPARED)	{
            if (DEBUG) System.out.println("waiting for client on port: " + port);

            setState(State.WAITING_FOR_CLIENT);
            if (!hostFileEdited)	{
                addToHostsFile(detourIP() + " " + input_domain);
            }

            proxy = new ServerSocket(port);

            try  {
                while ((state == State.WAITING_FOR_CLIENT) && !proxy.isClosed())	{
                    try {
                        Socket client = proxy.accept();
                        if (DEBUG) System.out.println("accepted a proxy");
                        new Thread(() -> {
                            try {
                                startProxyThread(client);
                            } catch (InterruptedException | IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }).start();


                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        //e1.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (DEBUG) System.out.println("done waiting for clients with: " + this.state );
        }
    }
    private void startProxyThread(Socket client) throws InterruptedException, UnknownHostException, IOException	{
        final boolean[] datastream = new boolean[1];

        Socket habbo_server = new Socket(actual_domain, port);

        OutputStream client_out = client.getOutputStream();
        InputStream client_in = client.getInputStream();
        OutputStream habbo_server_out = habbo_server.getOutputStream();
        InputStream habbo_server_in = habbo_server.getInputStream();

        if (DEBUG) System.out.println(habbo_server.getLocalAddress().getHostAddress() + ": " + habbo_server.getLocalPort());

        final boolean[] aborted = new boolean[1];

        Rc4Obtainer rc4Obtainer = new Rc4Obtainer();

        // wachten op data van client
        new Thread(() -> {
            try {
                OutgoingHandler handler = new OutgoingHandler(habbo_server_out);
                rc4Obtainer.setOutgoingHandler(handler);

                while (!client.isClosed() && (state == State.WAITING_FOR_CLIENT || state == State.CONNECTED)) {
                    byte[] buffer;
                    while (client_in.available() > 0)	{
                        client_in.read(buffer = new byte[client_in.available()]);

                        handler.act(buffer, trafficListeners);
                        if (!datastream[0] && handler.isDataStream())	{
                            datastream[0] = true;
                            setState(State.CONNECTED);
                            onConnect();

                            outHandler = handler;
                            //client_outputStream = client_out;
                            //server_outputStream = habbo_server_out;
                        }

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
                IncomingHandler handler = new IncomingHandler(client_out);
                rc4Obtainer.setIncomingHandler(handler);

                while (!habbo_server.isClosed() && (state == State.CONNECTED || state == State.WAITING_FOR_CLIENT)) {
                    byte[] buffer;
                    while (habbo_server_in.available() > 0) {
                        habbo_server_in.read(buffer = new byte[habbo_server_in.available()]);
                        if (!handler.isDataStream() && datastream[0]) {
                            handler.setAsDataStream();
                            inHandler = handler;
                        }
                        handler.act(buffer, trafficListeners);
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
        if (hostFileEdited)	{
            removeFromHostsFile(detourIP() + " " + input_domain);
        }

        if (proxy != null && !proxy.isClosed())	{
            try {
                proxy.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public void abort()	{
        if (hostFileEdited)	{
            removeFromHostsFile(detourIP() + " " + input_domain);
        }
        port = -1;
        setState(State.NOT_CONNECTED);
        actual_domain = null;
        if (proxy != null && !proxy.isClosed())	{
            try {
                proxy.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private void addToHostsFile(String text)	{
        if (DEBUG) System.out.println("try add hostsfile: " + text);
        try
        {
            ArrayList<String> lines = new ArrayList<String>();
            File f1 = new File(hostsFileLocation);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            boolean containmmm = false;
            while ((line = br.readLine()) != null)
            {
                if (line.equals(text))
                    containmmm = true;
                lines.add(line);

            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);

            if (!containmmm)	{
                out.write(text);
            }

            for (int i = 0; i < lines.size(); i++)	{
                out.write("\n"+ lines.get(i));
            }

            out.flush();
            out.close();
            hostFileEdited = true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
    private void removeFromHostsFile(String text)	{
        try
        {
            if (DEBUG) System.out.println("try remove hostsfile: " + text);
            ArrayList<String> lines = new ArrayList<String>();
            File f1 = new File(hostsFileLocation);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                if (!line.contains(text))
                    lines.add(line);

            }
            fr.close();
            br.close();

            FileWriter fw = new FileWriter(f1);
            BufferedWriter out = new BufferedWriter(fw);

            for (int i = 0; i < lines.size(); i++)	{
                out.write(lines.get(i));
                if (i != lines.size() - 1) out.write("\n");
            }
            out.flush();
            out.close();
            hostFileEdited = false;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    private void setState(State state) {
        if (state != this.state) {
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
     * ¹don't modificate (block, replace)
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
        return port;
    }
    public String getHost() {
        return actual_domain;
    }
    public String getDomain() { return input_domain; }


    public boolean sendToClient(HPacket message) {
        if (inHandler == null) return false;
        new Thread(() -> inHandler.sendToStream(message.toBytes())).start();
        return true;
    }
    public boolean sendToServer(HPacket message) {
        if (outHandler == null) return false;
        outHandler.sendToStream(message.toBytes());
        return false;
    }

    public void sendToClientAsync(HPacket message) {
        new Thread(() -> {
            sendToClient(message);
        }).start();
    }
    public void sendToServerAsync(HPacket message) {
        new Thread(() -> {
            sendToServer(message);
        }).start();
    }

}
