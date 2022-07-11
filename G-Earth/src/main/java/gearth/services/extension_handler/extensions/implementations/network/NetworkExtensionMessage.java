package gearth.services.extension_handler.extensions.implementations.network;

import gearth.misc.HostInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.services.packet_info.PacketInfoManager;

import java.util.List;

/**
 * Represents a message send or received by G-Earth and a remote extension.
 *
 * @see NetworkExtensionCodec the encoding/decoding structures
 * @see Incoming messages coming from the remote extension to G-Earth
 * @see Outgoing messages coming from G-Earth to the remote extension
 *
 * @author Dorving, Jonas
 */
public class NetworkExtensionMessage {

    /**
     * Represents {@link  NetworkExtensionMessage messages} coming from the remote extension to G-Earth.
     */
    public static class Incoming extends NetworkExtensionMessage {

        /**
         * This contains info about the remote extension trying to connect.
         *
         * Once this {@link NetworkExtensionMessage message} is received,
         * a new {@link  NetworkExtensionClient} is created to handle the communication.
         *
         * @see Outgoing.InfoRequest the request.
         */
        public static class ExtensionInfo extends Incoming {

            public static final int HEADER_ID = 1;

            private final String title;
            private final String author;
            private final String version;
            private final String description;
            private final boolean onClickUsed;
            private final String file;
            private final String cookie;
            private final boolean canLeave;
            private final boolean canDelete;

            public ExtensionInfo(String title, String author, String version, String description, boolean onClickUsed, String file, String cookie, boolean canLeave, boolean canDelete) {
                this.title = title;
                this.author = author;
                this.version = version;
                this.description = description;
                this.onClickUsed = onClickUsed;
                this.file = file;
                this.cookie = cookie;
                this.canLeave = canLeave;
                this.canDelete = canDelete;
            }

            public String getTitle() {
                return title;
            }

            public String getAuthor() {
                return author;
            }

            public String getVersion() {
                return version;
            }

            public String getDescription() {
                return description;
            }

            public boolean isOnClickUsed() {
                return onClickUsed;
            }

            public String getFile() {
                return file;
            }

            public String getCookie() {
                return cookie;
            }

            public boolean isCanLeave() {
                return canLeave;
            }

            public boolean isCanDelete() {
                return canDelete;
            }
        }

        /**
         * Remote extension request G-Earth's flags.
         *
         * @see Outgoing.FlagsCheck the response.
         */
        public static class RequestFlags extends Incoming {
            public static final int HEADER_ID = 3;
        }

        /**
         * Received a {@link HPacket} from the remote connection
         * and forward it either to the game {@link HMessage.Direction#TOSERVER server}
         * or game {@link  HMessage.Direction#TOCLIENT client}.
         */
        public static class SendMessage extends Incoming {

            public static final int HEADER_ID = 4;

            private final HPacket packet;
            private final HMessage.Direction direction;

            public SendMessage(HPacket packet, HMessage.Direction direction) {
                this.packet = packet;
                this.direction = direction;
            }

            public HPacket getPacket() {
                return packet;
            }

            public HMessage.Direction getDirection() {
                return direction;
            }
        }

        /**
         * TODO: add documentation.
         *
         * @see Outgoing.PacketToStringResponse the response.
         */
        public static class PacketToStringRequest extends Incoming {

            public static final int HEADER_ID = 20;

            private final String string;

            public PacketToStringRequest(String string) {
                this.string = string;
            }

            public String getString() {
                return string;
            }
        }

        /**
         * TODO: add documentation.
         *
         * @see Outgoing.StringToPacketResponse the response.
         */
        public static class StringToPacketRequest extends Incoming {

            public static final int HEADER_ID = 21;

            private final String string;

            public StringToPacketRequest(String string) {
                this.string = string;
            }

            public String getString() {
                return string;
            }
        }

        /**
         * TODO: add documentation.
         */
        public static class ExtensionConsoleLog extends Incoming {

            public static final int HEADER_ID = 98;

            private final String contents;

            public ExtensionConsoleLog(String contents) {
                this.contents = contents;
            }

            public String getContents() {
                return contents;
            }
        }

        /**
         * Represents a packet modified by the remote extension.
         *
         * @see Outgoing.PacketIntercept the ougoing message containing the original packet.
         */
        public static class ManipulatedPacket extends Incoming {
            public static final int MANIPULATED_PACKET = 2;
            private final HMessage hMessage;

            public ManipulatedPacket(HMessage hMessage) {
                this.hMessage = hMessage;
            }

            public HMessage gethMessage() {
                return hMessage;
            }
        }
    }
    /**
     * Represents {@link  NetworkExtensionMessage messages} coming from G-Earth to the remote extension.
     */
    public static class Outgoing extends NetworkExtensionMessage{

        /**
         * The extension has been double-clicked from within G-Earth.
         */
        public static class OnDoubleClick extends Outgoing {
            public static final int HEADER_ID = 1;
        }

        /**
         * Request for remote extension to send {@link Incoming.ExtensionInfo}.
         *
         * This is the very first message send after a connection is established.
         *
         * @see Incoming.ExtensionInfo the response.
         */
        public static class InfoRequest extends Outgoing {
            public static final int HEADER_ID = 2;
        }

        /**
         * Forwards a packet intercepted by G-Earth to the remote extension.
         *
         * @see Incoming.ManipulatedPacket the response.
         */
        public static class PacketIntercept extends Outgoing {

            public static final int HEADER_ID = 3;

            private final String packetString;

            public PacketIntercept(String packetString) {
                this.packetString = packetString;
            }

            public String getPacketString() {
                return packetString;
            }
        }

        /**
         * Contains program arguments of G-Earth.
         *
         * @see Incoming.RequestFlags the request.
         */
        public static class FlagsCheck extends Outgoing {

            public static final int HEADER_ID = 4;

            private final List<String> flags;

            public FlagsCheck(List<String> flags) {
                this.flags = flags;
            }

            public List<String> getFlags() {
                return flags;
            }
        }

        /**
         * Notifies remote extension that a connection to a hotel has been established.
         *
         * @apiNote could check this yourself as well (listen to out:4000 packet)
         */
        public static class ConnectionStart extends Outgoing {

            public static final int HEADER_ID = 5;

            private final String host;
            private final int connectionPort;
            private final String hotelVersion;
            private final String clientIdentifier;
            private final HClient clientType;
            private final PacketInfoManager packetInfoManager;

            public ConnectionStart(String host, int connectionPort, String hotelVersion, String clientIdentifier, HClient clientType, PacketInfoManager packetInfoManager) {
                this.host = host;
                this.connectionPort = connectionPort;
                this.hotelVersion = hotelVersion;
                this.clientIdentifier = clientIdentifier;
                this.clientType = clientType;
                this.packetInfoManager = packetInfoManager;
            }

            public String getHost() {
                return host;
            }

            public int getConnectionPort() {
                return connectionPort;
            }

            public String getHotelVersion() {
                return hotelVersion;
            }

            public String getClientIdentifier() {
                return clientIdentifier;
            }

            public HClient getClientType() {
                return clientType;
            }

            public PacketInfoManager getPacketInfoManager() {
                return packetInfoManager;
            }
        }

        /**
         * Notifies a remote extension that the connection to the hotel has been closed.
         */
        public static class ConnectionEnd extends Outgoing {
            public static final int HEADER_ID = 6;
        }

        /**
         * Notifies a remote extension that it has been accepted by G-Earth.
         */
        public static class Init extends Outgoing {

            public static final int HEADER_ID = 7;

            private final boolean delayInit;
            private final HostInfo hostInfo;

            public Init(boolean delayInit, HostInfo hostInfo) {
                this.delayInit = delayInit;
                this.hostInfo = hostInfo;
            }

            public boolean isDelayInit() {
                return delayInit;
            }

            public HostInfo getHostInfo() {
                return hostInfo;
            }
        }

        /**
         * TODO: add documentation.
         */
        public static class UpdateHostInfo extends Outgoing {

            public static final int HEADER_ID = 10;

            private final HostInfo hostInfo;

            public UpdateHostInfo(HostInfo hostInfo) {
                this.hostInfo = hostInfo;
            }

            public HostInfo getHostInfo() {
                return hostInfo;
            }
        }

        /**
         * TODO: add documentation.
         *
         * @see Incoming.PacketToStringRequest the request.
         */
        public static class PacketToStringResponse extends Outgoing {

            public static final int HEADER_ID = 20;

            private final String string;
            private final String expression;

            public PacketToStringResponse(String string, String expression) {
                this.string = string;
                this.expression = expression;
            }

            public String getString() {
                return string;
            }

            public String getExpression() {
                return expression;
            }
        }

        /**
         * TODO: add documentation.
         *
         * @see Incoming.StringToPacketRequest the request.
         */
        public static class StringToPacketResponse extends Outgoing {

            public static final int HEADER_ID = 21;

            private final String string;

            public StringToPacketResponse(String string) {
                this.string = string;
            }

            public String getString() {
                return string;
            }
        }
    }
}
