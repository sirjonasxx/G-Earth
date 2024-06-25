import gearth.protocol.HPacket;
import gearth.protocol.HPacketFormat;
import gearth.protocol.packethandler.shockwave.packets.ShockPacketOutgoing;
import gearth.services.packet_representation.PacketStringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPacketStringEncoding {

    private static final int OUT_CHAT = 52;

    @Test
    public void testWrite() throws Exception {
        // {h:OUT_CHAT}{s:"ç"} 40 40 45 40 74 40 41 e7
        checkPacket(new ShockPacketOutgoing(OUT_CHAT, "ç"), "40744041e7");
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, "@t@Aç", "40744041e7");
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, "@t@A[231]", "40744041e7");
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, String.format("{h:%d}{s:\"ç\"}", OUT_CHAT), "40744041e7");
        // {h:OUT_CHAT}{s:"ççç"} 40 40 47 40 74 40 43 e7 e7 e7
        checkPacket(new ShockPacketOutgoing(OUT_CHAT, "ççç"), "40744043e7e7e7");
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, "@t@Cççç", "40744043e7e7e7");
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, "@t@C[231][231][231]", "40744043e7e7e7");
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, String.format("{h:%d}{s:\"ççç\"}", OUT_CHAT), "40744043e7e7e7");
        // {h:OUT_CHAT}{s:"çãâéèä"} 40 40 47 40 74 40 46 e7 e3 e2 e9 e8 e4
        checkPacket(new ShockPacketOutgoing(OUT_CHAT, "çãâéèä"), "40744046e7e3e2e9e8e4");
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, String.format("{h:%d}{s:\"çãâéèä\"}", OUT_CHAT), "40744046e7e3e2e9e8e4");
    }

    @Test
    public void testReadIncoming() throws Exception {
        final String expectedString = "çãâéèä";

        final String packetData = "40585045e7e3e2e9e8e40201";
        final HPacket packet = HPacketFormat.WEDGIE_INCOMING.createPacket(Hex.decode(packetData));

        assertEquals(24, packet.headerId());
        assertEquals(20, packet.readInteger());
        assertEquals(expectedString, packet.readString());
    }

    @Test
    public void testReadOutgoing() throws Exception {
        final String expectedString = "çãâéèä";

        final String packetData = "40744046e7e3e2e9e8e4";
        final HPacket packet = HPacketFormat.WEDGIE_OUTGOING.createPacket(Hex.decode(packetData));

        assertEquals(OUT_CHAT, packet.headerId());
        assertEquals(expectedString, packet.readString());
    }

    /**
     * @param format Packet format.
     * @param expression Handwritten expression.
     * @param expectedHex Captured packets from the real client, or server.
     */
    private void checkExpression(HPacketFormat format, String expression, String expectedHex) throws Exception {
        final HPacket packet = PacketStringUtils.fromString(expression, format);
        final String packetData = Hex.toHexString(packet.toBytes());

        assertEquals(expectedHex, packetData);
    }

    /**
     * @param packet The packet.
     * @param expectedHex Captured packets from the real client, or server.
     */
    private void checkPacket(HPacket packet, String expectedHex) throws Exception {
        final String packetData = Hex.toHexString(packet.toBytes());

        assertEquals(expectedHex, packetData);
    }

}
