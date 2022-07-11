package gearth.services.extension_handler.extensions.implementations.network;

import gearth.misc.HostInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.connection.HClient;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionMessage.Outgoing;
import gearth.services.extension_handler.extensions.implementations.network.NetworkExtensionMessage.Incoming;
import gearth.services.packet_info.PacketInfoManager;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static gearth.protocol.HMessage.Direction.TOCLIENT;
import static gearth.protocol.HMessage.Direction.TOSERVER;

/**
 * THE EXTENSION COMMUNICATION PRINCIPLES & PROTOCOL:
 *
 * You will be able to write extensions in ANY language you want, but we will only provide an interface
 * for Java so if you write your own in for example Python, make SURE you do it correctly or it could fuck G-Earth.
 *
 * Also, don't let the method where you manipulate the packets block. Similiar as how you must not block things in an UI thread.
 * Why? Because Habbo relies on the TCP protocol, which ENSURES that packets get received in the right order, so we will not be fucking that up.
 * That means that all packets following the packet you're manipulating in your extension will be blocked from being sent untill you're done.
 * TIP: If you're trying to replace a packet in your extension but you know it will take time, just block the packet, end the method, and let something asynchronous send
 * the edited packet when you're done.
 *
 *
 * You may ignore everything beneath this line if you're extending the abstract Extension class we provide in Java.
 * -----------------------------------------------------------------------------------------------------------------
 *
 * (0. We recommend to use a cross-platform language for your extension)
 *
 * 1.   An extension will run as a seperate process on your device and has to be called with the flag "-p <PORT>",
 *      where <PORT> is a random port where the G-Earth local extension server will run on. Your extension has to connect with this server.
 *
 * 2.   G-Earth will open your program only ONCE, that is on the boot of G-Earth or when you install the exension.
 *      Same story goes for closing the connection between the program and G-Earth, only once (on uninstall or close of G-Earth).
 *
 *      You may also run your extension completely seperate from G-Earth for debugging purpose for example, then it won't be installed in G-Earth
 *      (but you have to configure the port yourself, which will be displayed in the extension page)
 *
 * 3.   Once a connection is made, your extension will have to deal with the following incoming & outgoing messages as described (follows the same protocol structure as Habbo communication does):
 *      (if an object is sent; the object will be sent with its String representation from the StringifyAble interface, so the object's class must implement that)
 *
 *      INCOMING MESSAGES: (marked with * if you're required to correctly respond or take action, ** if it's a response on something you requested)
 *      -----------------------------------------------------------------------------------------------------
 *      |  ID  |       TITLE        |                         BODY & DESCRIPTION                            |
 *      -----------------------------------------------------------------------------------------------------
 *      |  1   |   ON-DOUBLECLICK   |  No body, the extension has been double clicked from within G-Earth   |  ( <- typically for tanji-module-like extensions you will open the UI here)
 *      -----------------------------------------------------------------------------------------------------
 *      |  2   |    INFO-REQUEST*   | Needs response with extension info (name, desc, author, version, ..), |
 *      |      |                    |  exact implementation is found in the Java abstract Extension class   |
 *      -----------------------------------------------------------------------------------------------------
 *      |  3   | PACKET-INTERCEPT*  |       Includes the whole HMessage as body, needs response with the    |
 *      |      |                    |                  manipulated HMessage (OUTGOING id: 2)                |
 *      -----------------------------------------------------------------------------------------------------
 *      |  4   |   FLAGS-CHECK**    | Body: String with G-Earth's boot flags (args from static gearth method) |
 *      -----------------------------------------------------------------------------------------------------
 *      |  5   |  CONNECTION START  |             just a note that a new connection has been made,          |
 *      |      |                    |   you could check this yourself as well (listen to out:4000 packet)   |
 *      |      |                    |                      host/port, hotel version                         |
 *      -----------------------------------------------------------------------------------------------------
 *      |  6   |   CONNECTION END   |        Empty body, just a note that a connection has ended            |
 *      -----------------------------------------------------------------------------------------------------
 *      |  7   |        INIT        |        Empty body, a connection with G-Earth has been set up          |
 *      -----------------------------------------------------------------------------------------------------
 *      |  99  |     FREE FLOW      |                      extension-specific body                          |
 *      -----------------------------------------------------------------------------------------------------
 *
 *      OUTGOING MESSAGES: (marked with * if that is a response to one of the msgs above)
 *      -----------------------------------------------------------------------------------------------------
 *      |  ID  |       TITLE        |                         BODY & DESCRIPTION                            |
 *      -----------------------------------------------------------------------------------------------------
 *      |  1   |  EXTENSION-INFO*   |                      Response for INFO-REQUEST                        |
 *      -----------------------------------------------------------------------------------------------------
 *      |  2   | MANIPULATED-PACKET*|                    Response for PACKET-INTERCEPT                      |
 *      -----------------------------------------------------------------------------------------------------
 *      |  3   |   REQUEST-FLAGS    |  Request G-Earth's flags, results in incoming FLAGS-CHECK response    |
 *      -----------------------------------------------------------------------------------------------------
 *      |  4   |    SEND-MESSAGE    |   Body: HMessage object. Sends the HPacket wrapped in the HMessage    |
 *      |      |                    |                        to the client/server                           |
 *      -----------------------------------------------------------------------------------------------------
 *      |  99  |     FREE FLOW      |                      extension-specific body                          |
 *      -----------------------------------------------------------------------------------------------------
 *
 * 4.   Your extension will only appear in the extension list once the EXTENSION-INFO has been received by G-Earth
 */
public final class NetworkExtensionCodec {

    private final static Map<Class<?>, PacketStructure> outgoingPacketStructures = new HashMap<>();
    private final static Map<Integer, PacketStructure> incomingPacketStructures = new HashMap<>();

    public static PacketStructure getIncomingStructure(int headerId) {
        return incomingPacketStructures.get(headerId);
    }

    public static<T extends NetworkExtensionMessage> PacketStructure getOutgoingStructure(T message) {
        return outgoingPacketStructures.get(message.getClass());
    }

    static {
        // incoming
        register(Outgoing.InfoRequest.HEADER_ID,
                Outgoing.InfoRequest.class,
                (message, hPacket) -> {
                },
                (hPacket -> new Outgoing.InfoRequest()));
        register(Outgoing.ConnectionStart.HEADER_ID,
                Outgoing.ConnectionStart.class,
                (message, hPacket) -> {
                    hPacket.appendString(message.getHost());
                    hPacket.appendInt(message.getConnectionPort());
                    hPacket.appendString(message.getHotelVersion());
                    hPacket.appendString(message.getClientIdentifier());
                    hPacket.appendString(message.getClientType().name());
                    message.getPacketInfoManager().appendToPacket(hPacket);
                },
                (hPacket -> new Outgoing.ConnectionStart(
                        hPacket.readString(),
                        hPacket.readInteger(),
                        hPacket.readString(),
                        hPacket.readString(),
                        HClient.valueOf(hPacket.readString()),
                        PacketInfoManager.readFromPacket(hPacket)
                )));
        register(Outgoing.ConnectionEnd.HEADER_ID,
                Outgoing.ConnectionEnd.class,
                (message, hPacket) -> {
                },
                (hPacket -> new Outgoing.ConnectionEnd()));
        register(Outgoing.FlagsCheck.HEADER_ID,
                Outgoing.FlagsCheck.class,
                (message, hPacket) -> {
                    hPacket.appendInt(message.getFlags().size());
                    message.getFlags().forEach(hPacket::appendString);
                },
                (hPacket -> {
                    final int size = hPacket.readInteger();
                    final List<String> flags = new ArrayList<>();
                    for (int i = 0; i < size; i++)
                        flags.add(hPacket.readString());
                    return new Outgoing.FlagsCheck(flags);
                }));
        register(Outgoing.Init.HEADER_ID,
                Outgoing.Init.class,
                (message, hPacket) -> {
                    hPacket.appendBoolean(message.isDelayInit());
                    message.getHostInfo().appendToPacket(hPacket);
                },
                (hPacket -> new Outgoing.Init(hPacket.readBoolean(), HostInfo.fromPacket(hPacket))));
        register(Outgoing.OnDoubleClick.HEADER_ID,
                Outgoing.OnDoubleClick.class,
                (message, hPacket) -> {
                },
                (hPacket -> new Outgoing.OnDoubleClick()));
        register(Outgoing.PacketIntercept.HEADER_ID,
                Outgoing.PacketIntercept.class,
                (message, hPacket) -> hPacket.appendLongString(message.getPacketString()),
                (hPacket -> new Outgoing.PacketIntercept(hPacket.readLongString())));
        register(Outgoing.UpdateHostInfo.HEADER_ID,
                Outgoing.UpdateHostInfo.class,
                (message, hPacket) -> message.getHostInfo().appendToPacket(hPacket),
                (hPacket -> new Outgoing.UpdateHostInfo(HostInfo.fromPacket(hPacket))));
        register(Outgoing.PacketToStringResponse.HEADER_ID,
                Outgoing.PacketToStringResponse.class,
                (message, hPacket) -> {
                    hPacket.appendLongString(message.getString());
                    hPacket.appendLongString(message.getExpression(), StandardCharsets.UTF_8);
                },
                (hPacket -> new Outgoing.PacketToStringResponse(hPacket.readLongString(), hPacket.readLongString(StandardCharsets.UTF_8)))
        );
        register(Outgoing.StringToPacketResponse.HEADER_ID,
                Outgoing.StringToPacketResponse.class,
                (message, hPacket) -> hPacket.appendLongString(message.getString()),
                (hPacket -> new Outgoing.StringToPacketResponse(hPacket.readLongString()))
        );
        // outgoing
        register(Incoming.ExtensionInfo.HEADER_ID,
                Incoming.ExtensionInfo.class,
                (message, hPacket) -> {
                    hPacket.appendString(message.getTitle());
                    hPacket.appendString(message.getAuthor());
                    hPacket.appendString(message.getVersion());
                    hPacket.appendString(message.getDescription());
                    hPacket.appendBoolean(message.isOnClickUsed());
                    hPacket.appendBoolean(message.getFile() != null);
                    hPacket.appendString(Optional.ofNullable(message.getFile()).orElse(""));
                    hPacket.appendString(Optional.ofNullable(message.getCookie()).orElse(""));
                    hPacket.appendBoolean(message.isCanLeave());
                    hPacket.appendBoolean(message.isCanDelete());
                },
                (hPacket -> {
                    final String title = hPacket.readString();
                    final String author = hPacket.readString();
                    final String version = hPacket.readString();
                    final String description = hPacket.readString();
                    final boolean isOnClickUsed = hPacket.readBoolean();
                    final boolean hasFile = hPacket.readBoolean();
                    String file = hPacket.readString();
                    if (!hasFile)
                        file = null;
                    String cookie = hPacket.readString();
                    if (cookie.isEmpty())
                        cookie = null;
                    final boolean canLeave = hPacket.readBoolean();
                    final boolean canDelete = hPacket.readBoolean();
                    return new Incoming.ExtensionInfo(title, author, version, description, isOnClickUsed, file, cookie, canLeave, canDelete);
                }));
        register(Incoming.ManipulatedPacket.MANIPULATED_PACKET,
                Incoming.ManipulatedPacket.class,
                (message, hPacket) -> hPacket.appendLongString(message.gethMessage().stringify()),
                (hPacket -> {
                    final String packetString = hPacket.readLongString(6);
                    final HMessage hMessage = new HMessage(packetString);
                    return new Incoming.ManipulatedPacket(hMessage);
                }));
        register(Incoming.SendMessage.HEADER_ID,
                Incoming.SendMessage.class,
                ((message, hPacket) -> {
                    hPacket.appendByte((byte) (message.getDirection() == TOCLIENT ? 0 : 1));
                    hPacket.appendInt(message.getPacket().getBytesLength());
                    hPacket.appendBytes(message.getPacket().toBytes());
                }),
                (hPacket -> {
                    final byte side = hPacket.readByte();
                    final int length = hPacket.readInteger();
                    final byte[] data = hPacket.readBytes(length);
                    final HPacket packet = new HPacket(data);
                    return new Incoming.SendMessage(packet, side == 0 ? TOCLIENT : TOSERVER);
                }));
        register(Incoming.RequestFlags.HEADER_ID,
                Incoming.RequestFlags.class,
                (message, hPacket) -> {
                },
                (hPacket -> new Incoming.RequestFlags()));
        register(Incoming.ExtensionConsoleLog.HEADER_ID,
                Incoming.ExtensionConsoleLog.class,
                (message, hPacket) -> hPacket.appendString(message.getContents()),
                (hPacket -> new Incoming.ExtensionConsoleLog(hPacket.readString())));
        register(Incoming.PacketToStringRequest.HEADER_ID,
                Incoming.PacketToStringRequest.class,
                (message, hPacket) -> hPacket.appendLongString(message.getString()),
                (hPacket -> new Incoming.PacketToStringRequest(hPacket.readLongString())));
        register(Incoming.StringToPacketRequest.HEADER_ID,
                Incoming.StringToPacketRequest.class,
                (message, hPacket) -> hPacket.appendLongString(message.getString(), StandardCharsets.UTF_8),
                (hPacket -> new Incoming.StringToPacketRequest(hPacket.readLongString(StandardCharsets.UTF_8))));
    }

    private static <T extends NetworkExtensionMessage> void register(final int headerId, Class<T> tClass, BiConsumer<T, HPacket> writer, Function<HPacket, T> reader) {
        final PacketStructure packetStructure = new PacketStructure(headerId, tClass.getSimpleName(), writer, reader);
        if (tClass.getSuperclass() == Outgoing.class)
            incomingPacketStructures.put(headerId, packetStructure);
        else
            outgoingPacketStructures.put(tClass, packetStructure);
    }


    static class PacketStructure {

        private final int headerId;
        private final String name;
        private final BiConsumer<? extends NetworkExtensionMessage, HPacket> writer;
        private final Function<HPacket, ? extends NetworkExtensionMessage> reader;

        public PacketStructure(int headerId, String name, BiConsumer<? extends NetworkExtensionMessage, HPacket> writer, Function<HPacket, ? extends NetworkExtensionMessage> reader) {
            this.headerId = headerId;
            this.name = name;
            this.writer = writer;
            this.reader = reader;
        }

        public int getHeaderId() {
            return headerId;
        }

        public String getName() {
            return name;
        }

        public BiConsumer<? extends NetworkExtensionMessage, HPacket> getWriter() {
            return writer;
        }

        public Function<HPacket, ? extends NetworkExtensionMessage> getReader() {
            return reader;
        }
    }
}
