package gearth.protocol;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.connection.packetsafety.PacketSafetyManager;
import gearth.protocol.connection.packetsafety.SafePacketsContainer;
import gearth.protocol.connection.proxy.nitro.NitroProxyProvider;
import gearth.services.packet_info.PacketInfoManager;
import gearth.protocol.connection.HClient;
import gearth.protocol.connection.HProxy;
import gearth.protocol.connection.HState;
import gearth.protocol.connection.proxy.ProxyProvider;
import gearth.protocol.connection.proxy.ProxyProviderFactory;
import gearth.protocol.connection.proxy.flash.unix.LinuxRawIpFlashProxyProvider;
import gearth.protocol.connection.proxy.unity.UnityProxyProvider;
import gearth.services.extension_handler.ExtensionHandler;
import gearth.ui.GEarthProperties;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.util.function.Consumer;

public class HConnection {

    private volatile ExtensionHandler extensionHandler = null;

    private volatile Object[] trafficObservables = {new Observable<TrafficListener>(), new Observable<TrafficListener>(), new Observable<TrafficListener>()};

    @Deprecated
    private volatile Observable<StateChangeListener> stateObservable = new Observable<>();

    private final ObjectProperty<HState> stateProperty = new SimpleObjectProperty<>(HState.NOT_CONNECTED);

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

        PacketSafetyManager.PACKET_SAFETY_MANAGER.initialize(this);
    }

    public ObjectProperty<HState> stateProperty() {
        return stateProperty;
    }

    public HState getState() {
        return stateProperty.get();
    }

    private void setState(HState state) {
        stateProperty.set(state);
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

    public void startNitro() {
        HConnection selff = this;
        proxyProvider = new NitroProxyProvider(proxy -> selff.proxy = proxy, selff::setState, this);
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
            setState(HState.ABORTING);
            setState(HState.NOT_CONNECTED);
        }
    }

    public void abort()	{
        if (proxyProvider != null) {
            proxyProvider.abort();
            proxyProvider = null;
        }
    }

    @Deprecated
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
        if (!canSendPacket(HMessage.Direction.TOCLIENT, packet)) return false;
        return proxy.sendToClient(packet);
    }

    public boolean sendToServer(HPacket packet) {
        if (!canSendPacket(HMessage.Direction.TOSERVER, packet)) return false;
        return proxy.sendToServer(packet);
    }

    public boolean canSendPacket(HMessage.Direction direction, HPacket packet) {
        return isPacketSendingAllowed(direction, packet)
                && (GEarthProperties.isDeveloperModeEnabled() || isPacketSendingSafe(direction, packet));
    }

    public boolean isPacketSendingAllowed(HMessage.Direction direction, HPacket packet) {
        if (getState() != HState.CONNECTED) return false;

        HProxy proxy = this.proxy;
        if (proxy == null) return false;
        if (packet.isCorrupted()) return false;

        if (!packet.isPacketComplete()) {
            PacketInfoManager packetInfoManager = getPacketInfoManager();
            packet.completePacket(packetInfoManager);

            return packet.isPacketComplete() &&
                    (packet.canSendToClient() || direction != HMessage.Direction.TOCLIENT) &&
                    (packet.canSendToServer() || direction != HMessage.Direction.TOSERVER);
        }

        return true;
    }

    public boolean isPacketSendingSafe(HMessage.Direction direction, HPacket packet) {
        if (proxy == null) return true; // do not mark unsafe, but won't pass "isPacketSendingAllowed()" check
        String hotelVersion = proxy.getHotelVersion();
        if (hotelVersion == null) return true;

        SafePacketsContainer packetsContainer = PacketSafetyManager.PACKET_SAFETY_MANAGER.getPacketContainer(hotelVersion);
        return packetsContainer.isPacketSafe(packet.headerId(), direction);
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
        return proxy.getHClient();
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
