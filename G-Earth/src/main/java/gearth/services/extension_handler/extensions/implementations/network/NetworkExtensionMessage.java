package gearth.services.extension_handler.extensions.implementations.network;

import gearth.misc.HostInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.services.packet_info.PacketInfoManager;

import java.util.List;

public class NetworkExtensionMessage {

    public static class Incoming extends NetworkExtensionMessage {

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

        public static class RequestFlags extends Incoming {
            public static final int HEADER_ID = 3;
        }

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

    public static class Outgoing extends NetworkExtensionMessage{

        public static class OnDoubleClick extends Outgoing {
            public static final int HEADER_ID = 1;
        }

        public static class InfoRequest extends Outgoing {
            public static final int HEADER_ID = 2;
        }

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

        public static class ConnectionEnd extends Outgoing {
            public static final int HEADER_ID = 6;
        }

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
