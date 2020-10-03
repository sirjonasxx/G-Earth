package gearth.services.extensionhandler.extensions.implementations.network;

import gearth.protocol.HMessage;
import gearth.services.extensionhandler.extensions.GEarthExtension;
import gearth.protocol.HPacket;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by Jonas on 21/06/18.
 */
public class NetworkExtension extends GEarthExtension {

    private String title;
    private String author;
    private String version;
    private String description;

    private boolean fireEventButtonVisible;
    private boolean leaveButtonVisible;
    private boolean deleteButtonVisible;

    private boolean isInstalledExtension; // <- extension is in the extensions directory
    private String fileName;
    private String cookie;

    private Socket connection;

    NetworkExtension(HPacket extensionInfo, Socket connection) {
        this.title = extensionInfo.readString();
        this.author = extensionInfo.readString();
        this.version = extensionInfo.readString();
        this.description = extensionInfo.readString();
        this.fireEventButtonVisible = extensionInfo.readBoolean();

        this.isInstalledExtension = extensionInfo.readBoolean();
        this.fileName = extensionInfo.readString();
        this.cookie = extensionInfo.readString();

        this.leaveButtonVisible = extensionInfo.readBoolean();
        this.deleteButtonVisible = extensionInfo.readBoolean();

        this.connection = connection;

        NetworkExtension selff = this;
        new Thread(() -> {
            try {
                InputStream inputStream = connection.getInputStream();
                DataInputStream dIn = new DataInputStream(inputStream);

                while (!connection.isClosed()) {
                    int length = dIn.readInt();
                    byte[] headerandbody = new byte[length + 4];

                    int amountRead = 0;
                    while (amountRead < length) {
                        amountRead += dIn.read(headerandbody, 4 + amountRead, Math.min(dIn.available(), length - amountRead));
                    }

                    HPacket message = new HPacket(headerandbody);
                    message.fixLength();

                    synchronized (selff.extensionObservable) {
                        if (message.headerId() == NetworkExtensionInfo.INCOMING_MESSAGES_IDS.REQUESTFLAGS) {
                            requestFlags();
                        }
                        else if (message.headerId() == NetworkExtensionInfo.INCOMING_MESSAGES_IDS.SENDMESSAGE) {
                            byte side = message.readByte();
                            int byteLength = message.readInteger();
                            byte[] packetAsByteArray = message.readBytes(byteLength);

                            HPacket packet = new HPacket(packetAsByteArray);
                            if (!packet.isCorrupted()) {
                                sendMessage(
                                        side == 0 ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER,
                                        packet
                                );
                            }
                        }
                        else if (message.headerId() == NetworkExtensionInfo.INCOMING_MESSAGES_IDS.MANIPULATEDPACKET) {
                            String stringifiedresponse = message.readLongString(6);
                            HMessage responseMessage = new HMessage(stringifiedresponse);
                            sendManipulatedPacket(responseMessage);
                        }
                        else if (message.headerId() == NetworkExtensionInfo.INCOMING_MESSAGES_IDS.EXTENSIONCONSOLELOG) {
                            log(message.readString());
                        }
                        else if (message.headerId() == NetworkExtensionInfo.INCOMING_MESSAGES_IDS.PACKETTOSTRING_REQUEST) {
                            HPacket p = new HPacket(new byte[0]);
                            p.constructFromString(message.readLongString());
                            packetToStringRequest(p);
                        }
                        else if (message.headerId() == NetworkExtensionInfo.INCOMING_MESSAGES_IDS.STRINGTOPACKET_REQUEST) {
                            stringToPacketRequest(message.readLongString());
                        }

                    }

                }

            } catch (IOException e) {
                // An extension disconnected, which is OK
            } finally {
                synchronized (selff.extensionObservable) {
                    hasClosed();
                }
                if (!connection.isClosed()) {
                    try {
                        connection.close();
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                }
            }
        }).start();


    }


    public String getAuthor() {
        return author;
    }
    public String getDescription() {
        return description;
    }
    public String getTitle() {
        return title;
    }
    public String getVersion() {
        return version;
    }
    public boolean isFireButtonUsed() {
        return fireEventButtonVisible;
    }
    public String getFileName() {
        return fileName;
    }
    public String getCookie() {
        return cookie;
    }
    public boolean isDeleteButtonVisible() {
        return deleteButtonVisible;
    }
    public boolean isLeaveButtonVisible() {
        return leaveButtonVisible;
    }

    public boolean isInstalledExtension() {
        return isInstalledExtension;
    }


    private boolean sendMessage(HPacket message) {
        try {
            synchronized (this) {
                connection.getOutputStream().write(message.toBytes());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void doubleclick() {
        sendMessage(new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.ONDOUBLECLICK));
    }

    @Override
    public void packetIntercept(HMessage hMessage) {
        String stringified = hMessage.stringify();
        HPacket manipulatePacketRequest = new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.PACKETINTERCEPT);
        manipulatePacketRequest.appendLongString(stringified);
        sendMessage(manipulatePacketRequest);
    }

    @Override
    public void provideFlags(String[] flags) {
        HPacket packet = new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.FLAGSCHECK);
        packet.appendInt(flags.length);
        for (String flag : flags) {
            packet.appendString(flag);
        }
        sendMessage(packet);
    }

    @Override
    public void connectionStart(String host, int port, String hotelVersion, String harbleMessagesPath) {
        sendMessage(
                new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.CONNECTIONSTART)
                        .appendString(host)
                        .appendInt(port)
                        .appendString(hotelVersion)
                        .appendString(harbleMessagesPath)
        );
    }

    @Override
    public void connectionEnd() {
        sendMessage(
                new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.CONNECTIONEND)
        );
    }

    @Override
    public void init() {
        sendMessage(
                new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.INIT)
        );
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (IOException ignored) { }
    }

    @Override
    public void packetToStringResponse(String string, String expression) {
        HPacket packet = new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.PACKETTOSTRING_RESPONSE);
        packet.appendLongString(string);
        packet.appendLongString(expression);
        sendMessage(packet);
    }

    @Override
    public void stringToPacketResponse(HPacket packetFromString) {
        HPacket packet = new HPacket(NetworkExtensionInfo.OUTGOING_MESSAGES_IDS.STRINGTOPACKET_RESPONSE);
        packet.appendLongString(packet.stringify());
        sendMessage(packet);
    }
}