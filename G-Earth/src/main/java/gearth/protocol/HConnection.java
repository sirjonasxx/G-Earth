package gearth.protocol;

import gearth.misc.listenerpattern.Observable;
import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.connection.HClient;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.flash.unix.LinuxRawIpFlashProxyProvider;
import gearth.protocol.connection.proxy.unity.UnityProxyProvider;
import gearth.services.extensionhandler.ExtensionHandler;

import java.io.IOException;

public class HConnection {

    public static volatile boolean DECRYPTPACKETS = true;
    public static volatile boolean DEBUG = false;

    private volatile ExtensionHandler extensionHandler = null;

    private volatile Object[] trafficObservables = {new Observable<TrafficListener>(), new Observable<TrafficListener>(), new Observable<TrafficListener>()};
    private volatile Observable<StateChangeListener> stateObservable = new Observable<>();

    private volatile HState state = HState.NOT_CONNECTED;
    private volatile HProxy proxy = null;

    private ProxyProviderFactory proxyProviderFactory;
    private ProxyProvider proxyProvider = null;

    public HConnection() {
        HConnection selff = this;
        proxyProviderFactory = new ProxyProviderFactory(
                proxy -> selff.proxy = proxy,
                selff::setState,
                this
        );
    }

    public HState getState() {
        return state;
    }

    private void setState(HState state) {
        if (state != this.state) {
            HState buffer = this.state;
            this.state = state;
            stateObservable.fireEvent(l -> l.stateChanged(buffer, state));
        }
    }

    // autodetect mode
    public void start() {
        proxyProvider = proxyProviderFactory.provide();
        startMITM();
    }

    // manual input mode
    public void start(String host, int port) {
        proxyProvider = proxyProviderFactory.provide(host, port);
        startMITM();
    }

    public void startUnity() {
        HConnection selff = this;
        proxyProvider = new UnityProxyProvider(proxy -> selff.proxy = proxy, selff::setState, this);
        startMITM();
    }

    private void startMITM() {
        try {
            if (proxyProvider != null) {
                proxyProvider.start();
            }
            else {
                // trigger UI update
                setState(HState.ABORTING);
                setState(HState.NOT_CONNECTED);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void abort()	{
        if (proxyProvider != null) {
            proxyProvider.abort();
            proxyProvider = null;
        }
    }

    public Observable<StateChangeListener> getStateObservable() {
        return stateObservable;
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
        ((Observable<TrafficListener>) trafficObservables[order]).addListener(listener);
    }
    public void removeTrafficListener(TrafficListener listener) {
        ((Observable<TrafficListener>) trafficObservables[0]).removeListener(listener);
        ((Observable<TrafficListener>) trafficObservables[1]).removeListener(listener);
        ((Observable<TrafficListener>) trafficObservables[2]).removeListener(listener);
    }

    public void setExtensionHandler(ExtensionHandler handler) {
        this.extensionHandler = handler;
    }

    public ExtensionHandler getExtensionHandler() {
        return extensionHandler;
    }

    public Object[] getTrafficObservables() {
        return trafficObservables;
    }

    public int getServerPort() {
        if (proxy == null) return -1;
        return proxy.getIntercept_port();
    }
    public String getServerHost() {
        if (proxy == null) return "<auto-detect>";
        return proxy.getActual_domain();
    }
    public String getDomain() {
        if (proxy == null) return "<auto-detect>";
        return proxy.getInput_domain();
    }


    public boolean sendToClient(HPacket packet) {
        if (proxy == null) return false;

        if (!packet.isPacketComplete()) {
            PacketInfoManager packetInfoManager = getPacketInfoManager();
            packet.completePacket(packetInfoManager);

            if (!packet.isPacketComplete() || !packet.canSendToClient()) return false;
        }

        proxy.getPacketSenderQueue().queueToClient(packet);
        return true;
    }
    public boolean sendToServer(HPacket packet) {
        if (proxy == null) return false;

        if (!packet.isPacketComplete()) {
            PacketInfoManager packetInfoManager = getPacketInfoManager();
            packet.completePacket(packetInfoManager);

            if (!packet.isPacketComplete() || !packet.canSendToServer()) return false;
        }

        proxy.getPacketSenderQueue().queueToServer(packet);
        return true;
    }

    public String getClientHost() {
        if (proxy == null) {
            return "";
        }
        return proxy.getIntercept_host();
    }

    public int getClientPort() {
        if (proxy == null) {
            return -1;
        }
        return proxy.getIntercept_port();
    }

    public String getHotelVersion() {
        if (proxy == null) {
            return "";
        }
        return proxy.getHotelVersion();
    }

    public String getClientIdentifier() {
        if (proxy == null) {
            return "";
        }
        return proxy.getClientIdentifier();
    }

    public HClient getClientType() {
        if (proxy == null) {
            return null;
        }
        return proxy.gethClient();
    }

    public PacketInfoManager getPacketInfoManager() {
        if (proxy == null) {
            return null;
        }
        return proxy.getPacketInfoManager();
    }

    public boolean isRawIpMode() {
        return proxyProvider != null && proxyProvider instanceof LinuxRawIpFlashProxyProvider;
        // WindowsRawIpProxyProvider extends LinuxRawIpProxyProvider
    }

    public ProxyProvider getProxyProvider() {
        return proxyProvider;
    }
}
