import gearth.protocol.HPacketFormat;
import gearth.services.packet_representation.InvalidPacketException;
import gearth.services.packet_representation.PacketStringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPacketStringUtils {

    // Important to test:
    // - bool
    // - byte
    // - int
    // - str

    @Test
    public void testShockwaveIncoming() throws InvalidPacketException {
        checkSame("@t@FHoi123");
        checkSame("AK@J@F");
        checkSame("@XHaaaaaaaaa[2]");
        checkSame("C\\HSGHDance Clubs & Pubs[2]HRLKSAIThe bobba Duck Pub[2]HRLSGthe_dirty_duck_pub[2]SAHhh_room_pub[2]HIRNIThe Chromide Club[2]IRLSGthe_chromide_club[2]RNHhh_room_disco[2]HI");
    }

    @Test
    public void testShockwaveIncomingExpressions() throws InvalidPacketException {
        checkExpression(HPacketFormat.WEDGIE_INCOMING, "{h:24}{i:0}{s:\"aaaaaaaaa\"}", "@XHaaaaaaaaa[2]");
        checkExpression(HPacketFormat.WEDGIE_INCOMING, "{h:34}{i:1}{i:4}{i:3}{i:5}{s:\"0.0\"}{i:2}{i:2}{s:\"/flatctrl 4/\"}", "@bIPAKQA0.0[2]JJ/flatctrl 4/[2]"); // STATUS
        checkExpression(HPacketFormat.WEDGIE_INCOMING, "{h:220}{i:0}" +
                        "{i:31}{i:0}{s:\"Dance Clubs & Pubs\"}{i:0}{i:50}{i:3}" +
                        "{i:7}{i:1}{s:\"The bobba Duck Pub\"}{i:0}{i:50}{i:31}{s:\"the_dirty_duck_pub\"}{i:7}{i:0}{s:\"hh_room_pub\"}{i:0}{b:true}" +
                        "{i:58}{i:1}{s:\"The Chromide Club\"}{i:1}{i:50}{i:31}{s:\"the_chromide_club\"}{i:58}{i:0}{s:\"hh_room_disco\"}{i:0}{b:true}",
                "C\\HSGHDance Clubs & Pubs[2]HRLKSAIThe bobba Duck Pub[2]HRLSGthe_dirty_duck_pub[2]SAHhh_room_pub[2]HIRNIThe Chromide Club[2]IRLSGthe_chromide_club[2]RNHhh_room_disco[2]HI"); // NAVNODEINFO
    }

    @Test
    public void testShockwaveOutgoingExpressions() throws InvalidPacketException {
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, "{h:18}{b:false}", "@RH"); // GETFVRF
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, "{h:52}{s:\"Hoi123\"}", "@t@FHoi123"); // CHAT
        checkExpression(HPacketFormat.WEDGIE_OUTGOING, "{h:75}{u:10}{u:6}", "AK@J@F"); // MOVE
    }

    private void checkSame(String expected) throws InvalidPacketException {
        // Should be the same for all formats
        for (HPacketFormat value : HPacketFormat.values()) {
            assertEquals(expected, PacketStringUtils.fromString(expected, value).toString());
        }
    }

    private void checkExpression(HPacketFormat format, String expression, String expected) throws InvalidPacketException {
        assertEquals(expected, PacketStringUtils.fromString(expression, format).toString());
    }

}
