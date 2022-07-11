package gearth.services.extension_handler.extensions.implementations.network;

import gearth.misc.HostInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.services.extension_handler.extensions.ExtensionType;
import gearth.services.extension_handler.extensions.GEarthExtension;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionMessage.Incoming;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionMessage.Outgoing;
import gearth.services.packet_info.PacketInfoManager;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public final class NetworkExtensionClient extends GEarthExtension {

    private final static Logger LOGGER = LoggerFactory.getLogger(NetworkExtensionClient.class);

    private final String title;
    private final String author;
    private final String version;
    private final String description;
    private final boolean fireEventButtonVisible;
    private final boolean leaveButtonVisible;
    private final boolean deleteButtonVisible;
    private final boolean isInstalledExtension;
    private final String fileName;
    private final String cookie;
    private final Channel channel;

    public NetworkExtensionClient(Incoming.ExtensionInfo msg, Channel channel) {
        title = msg.getTitle();
        author = msg.getAuthor();
        version = msg.getVersion();
        description = msg.getDescription();
        fireEventButtonVisible = msg.isOnClickUsed();
        leaveButtonVisible = msg.isCanLeave();
        deleteButtonVisible = msg.isCanDelete();
        isInstalledExtension = msg.getFile() != null;
        fileName = msg.getFile();
        cookie = msg.getCookie();
        this.channel = channel;
    }

    public void handleIncomingMessage(Incoming msg) {
        try {
            if (msg instanceof Incoming.RequestFlags)
                requestFlags();
            else if (msg instanceof Incoming.SendMessage) {
                final Incoming.SendMessage message = ((Incoming.SendMessage) msg);
                final HPacket packet = message.getPacket();
                if (!packet.isCorrupted())
                    sendMessage(message.getDirection(), packet);
            } else if (msg instanceof Incoming.ManipulatedPacket) {
                sendManipulatedPacket(((Incoming.ManipulatedPacket) msg).gethMessage());
            } else if (msg instanceof Incoming.ExtensionConsoleLog) {
                log(((Incoming.ExtensionConsoleLog) msg).getContents());
            } else if (msg instanceof Incoming.PacketToStringRequest) {
                final HPacket hPacket = new HPacket(new byte[0]);
                hPacket.constructFromString(((Incoming.PacketToStringRequest) msg).getString());
                packetToStringRequest(hPacket);
            } else if (msg instanceof Incoming.StringToPacketRequest) {
                stringToPacketRequest(((Incoming.StringToPacketRequest) msg).getString());
            }
        } catch (Exception e){
            LOGGER.error("Failed to handle incoming message {} (channel={})", msg, channel, e);
        }
    }

    @Override
    public void init(boolean isConnected, HostInfo hostInfo) {
        channel.writeAndFlush(new Outgoing.Init(isConnected, hostInfo));
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (Exception e){
            LOGGER.error("Failed to close client (channel={})", channel, e);
        }
    }

    @Override
    public void connectionStart(String host, int port, String hotelVersion, String clientIdentifier, HClient clientType, PacketInfoManager packetInfoManager) {
        channel.writeAndFlush(new Outgoing.ConnectionStart(
                host,
                port,
                hotelVersion,
                clientIdentifier,
                clientType,
                packetInfoManager
        ));
    }

    @Override
    public void connectionEnd() {
        channel.writeAndFlush(new Outgoing.ConnectionEnd());
    }

    @Override
    public void doubleclick() {
        channel.writeAndFlush(new Outgoing.OnDoubleClick());
    }

    @Override
    public void provideFlags(String[] flags) {
        channel.writeAndFlush(new Outgoing.FlagsCheck(Arrays.asList(flags)));
    }

    @Override
    public void updateHostInfo(HostInfo hostInfo) {
        channel.writeAndFlush(new Outgoing.UpdateHostInfo(hostInfo));
    }

    @Override
    public void packetIntercept(HMessage hMessage) {
        final String messageAsString = hMessage.stringify();
        channel.writeAndFlush(new Outgoing.PacketIntercept(messageAsString));
    }

    @Override
    public void packetToStringResponse(String string, String expression) {
        channel.writeAndFlush(new Outgoing.PacketToStringResponse(string, expression));
    }

    @Override
    public void stringToPacketResponse(HPacket packet) {
        channel.writeAndFlush(new Outgoing.StringToPacketResponse(packet.stringify()));
    }

    @Override
    public ExtensionType extensionType() {
        return ExtensionType.EXTERNAL;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    public String getCookie() {
        return cookie;
    }

    @Override
    public boolean isFireButtonUsed() {
        return fireEventButtonVisible;
    }

    @Override
    public boolean isDeleteButtonVisible() {
        return deleteButtonVisible;
    }

    @Override
    public boolean isLeaveButtonVisible() {
        return leaveButtonVisible;
    }

    @Override
    public boolean isInstalledExtension() {
        return isInstalledExtension;
    }
}
