package gearth.services.packet_representation;

import gearth.protocol.HMessage;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_representation.prediction.StructurePredictor;
import gearth.protocol.HPacket;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// for all the logistics behind bytes-string conversion
public class PacketStringUtils {

    private static String replaceAll(String templateText, String regex,
                                           Function<Matcher, String> replacer) {
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(templateText);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(replacer.apply(matcher))
            );
        }
        matcher.appendTail(result);
        return result.toString();
    }


    public static HPacket fromString(String packet) throws InvalidPacketException {
        boolean fixLengthLater = false;
        if (packet.startsWith("{l}")) {
            fixLengthLater = true;
            packet = packet.substring(3);
        }

        // note: in String expressions {s:"string"}, character " needs to be backslashed -> \" if used in string
        packet = replaceAll(packet, "\\{i:(-?[0-9]+)}",
                m -> toString(ByteBuffer.allocate(4).putInt(Integer.parseInt(m.group(1))).array()));

        packet = replaceAll(packet, "\\{l:(-?[0-9]+)}",
                m -> toString(ByteBuffer.allocate(8).putLong(Long.parseLong(m.group(1))).array()));

        packet = replaceAll(packet, "\\{d:(-?[0-9]*\\.[0-9]*)}",
                m -> toString(ByteBuffer.allocate(8).putDouble(Double.parseDouble(m.group(1))).array()));

        packet = replaceAll(packet, "\\{u:([0-9]+)}",
                m -> "[" + (Integer.parseInt(m.group(1))/256) + "][" + (Integer.parseInt(m.group(1)) % 256) + "]");

        packet = replaceAll(packet, "\\{h:(-?[0-9]+)}",
                m -> toString(ByteBuffer.allocate(2).putShort(Short.parseShort(m.group(1))).array()));

        packet = replaceAll(packet, "\\{b:([Ff]alse|[Tt]rue)}",
                m -> m.group(1).toLowerCase().equals("true") ? "[1]" : "[0]");

        packet = replaceAll(packet, "\\{b:([0-9]{1,3})}",
                m -> "[" + Integer.parseInt(m.group(1)) + "]");

        // results in regex stackoverflow for long strings
//        packet = replaceAll(packet, "\\{s:\"(([^\"]|(\\\\\"))*)\"}",
//                m -> {
//            String match = m.group(1).replace("\\\"", "\"");
//            return toString(new HPacket(0, match).readBytes(match.length() + 2, 6));
//        });

        while (packet.contains("{s:\"")) {
            int start = packet.indexOf("{s:\"");
            int end = -1;
            boolean valid;
            do {
                end = packet.indexOf("\"}", end + 1);

                valid = false;
                if (end != -1) {
                    int amountBackslashes = 0;
                    int pos = end - 1;

                    while (pos >= start + 4 && packet.charAt(pos) == '\\') {
                        amountBackslashes++;
                        pos -= 1;
                    }

                    valid = amountBackslashes % 2 == 0;
                }
            } while (end != -1 && !valid);

            if (end == -1) {
                throw new InvalidPacketException();
            }

//            String match = packet.substring(start + 4, end).replace("\\\"", "\"");
            String match = packet.substring(start + 4, end);
            StringBuilder actualString = new StringBuilder();

            while (match.contains("\\")) {
                int index = match.indexOf("\\");
                // we know this can't be the last character
                char c = match.charAt(index + 1);
                actualString.append(match, 0, index);

                switch (c) {
                    // all characters that can be backslashed
                    case '"': {
                        actualString.append('"');
                        match = match.substring(index + 2);
                        break;
                    }
                    case 'r': {
                        actualString.append('\r');
                        match = match.substring(index + 2);
                        break;
                    }
                    case '\\': {
                        actualString.append('\\');
                        match = match.substring(index + 2);
                        break;
                    }
                    default: {
//                        actualString.append('\\');
//                        match = match.substring(index + 1);
                        throw new InvalidPacketException();
                    }
                }
            }
            actualString.append(match);

            String latin = new String(actualString.toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
            HPacket temp = new HPacket(0);
            temp.appendString(latin, StandardCharsets.ISO_8859_1);

            packet = packet.substring(0, start) +
                    toString(temp.readBytes(latin.length() + 2, 6)) +
                    packet.substring(end + 2);
        }

        String[] identifier = {null};
        if (!fixLengthLater && packet.startsWith("{")) {
            packet = replaceAll(packet, "^\\{((in|out):[^:{}]*)}", m -> {
                identifier[0] = m.group(1);
                return "[255][255]";
            });
        }
        if (identifier[0] != null) fixLengthLater = true;

        if (packet.contains("{") || packet.contains("}")) {
            throw new InvalidPacketException();
        }

        boolean[] corrupted = new boolean[]{false};
        packet = replaceAll(packet, "\\[([0-9]{1,3})]", m -> {
            int b = Integer.parseInt(m.group(1));
            if (b < 0 || b >= 256) {
                corrupted[0] = true;
                return "";
            }
            return new String(new byte[]{(byte) (b > 127 ? b - 256 : b)}, StandardCharsets.ISO_8859_1);
        });
        if (corrupted[0]) {
            throw new InvalidPacketException();
        }

        byte[] packetInBytes = packet.getBytes(StandardCharsets.ISO_8859_1);
        if (fixLengthLater) {
            byte[] combined = new byte[4 + packetInBytes.length];
            System.arraycopy(packetInBytes,0, combined, 4, packetInBytes.length);
            packetInBytes = combined;
        }

        HPacket hPacket = new HPacket(packetInBytes);
        if (fixLengthLater) {
            hPacket.fixLength();
        }
        if (identifier[0] != null) {
            String[] split = identifier[0].split(":");
            hPacket.setIdentifierDirection(split[0].equals("in") ? HMessage.Direction.TOCLIENT : HMessage.Direction.TOSERVER);
            hPacket.setIdentifier(split[1]);
        }
        return hPacket;
    }
    public static String toString(byte[] packet) {
        StringBuilder teststring = new StringBuilder();
        for (byte x : packet)	{
            if ((x < 32 && x >= 0) || x < -96 || x == 93 || x == 91 || x == 125 || x == 123 || x == 127 )
                teststring.append("[").append((((int) x) + 256) % 256).append("]");
            else {
                teststring.append(new String(new byte[]{x}, StandardCharsets.ISO_8859_1));
            }
        }
        return teststring.toString();
    }

    // generates an expression for a packet from a packet structure (ex. "i(isi(b))iBd")
    public static String toExpressionFromGivenStructure(HPacket packet, String struct, PacketInfo packetInfo) {
        int oldReadIndex = packet.getReadIndex();
        packet.resetReadIndex();

        StringBuilder builder = new StringBuilder();
        if (packetInfo != null) {
            String identifier = packetInfo.getName() == null ? packetInfo.getHash() : packetInfo.getName();
            builder.append("{").append(packetInfo.getDestination() == HMessage.Direction.TOCLIENT ? "in:" : "out:").append(identifier).append("}");
        }
        else {
            builder.append("{l}{h:").append(packet.headerId()).append("}");
        }

        buildExpressionFromGivenStructure(packet, struct, 0, builder);

        packet.setReadIndex(oldReadIndex);
        return builder.toString();
    }
    private static void buildExpressionFromGivenStructure(HPacket p, String struct, int indexInGivenStruct, StringBuilder builder) {
        int prevInt = 0;

        while (indexInGivenStruct < struct.length()) {
            char c = struct.charAt(indexInGivenStruct++);
            if (c == '(') {
                for (int i = 0; i < prevInt; i++) buildExpressionFromGivenStructure(p, struct, indexInGivenStruct, builder);
                int skipping = 1;
                while (skipping > 0) {
                    char c2 = struct.charAt(indexInGivenStruct++);
                    if (c2 == '(') skipping++;
                    else if (c2 == ')') skipping--;
                }
            }
            else if (c == 'i') builder.append("{i:").append(prevInt = p.readInteger()).append('}');
            else if (c == 's') builder.append("{s:\"").append(
                    p.readString(StandardCharsets.UTF_8)
                            .replace("\\", "\\\\")  // \ -> \\
                            .replace("\"", "\\\"")  // " -> \"
                            .replace("\r", "\\r")   // CR -> \r
            ).append("\"}");
            else if (c == 'd') builder.append("{d:").append(p.readDouble()).append('}');
            else if (c == 'f') builder.append("{f:").append(p.readFloat()).append('}');
            else if (c == 'b') builder.append("{b:").append((((int)(p.readByte())) + 256) % 256).append('}');
            else if (c == 'B') builder.append("{b:").append(p.readBoolean()).append('}');
            else if (c == 'l') builder.append("{l:").append(p.readLong()).append('}');
            else if (c == 'u') builder.append("{u:").append(p.readShort()).append('}');
            else return;
        }
    }
    public static String predictedExpression(HPacket packet, PacketInfo packetInfo) {
        StructurePredictor structurePredictor = new StructurePredictor(packet);
        String structure = structurePredictor.getStructure();
        if (structure == null) return "";

        return PacketStringUtils.toExpressionFromGivenStructure(packet, structure, packetInfo);
    }

    public static boolean structureEquals(HPacket packet, String struct) {
        if (packet.isCorrupted()) return false;

        int indexbuffer = packet.getReadIndex();
        packet.resetReadIndex();

        boolean result;
        try {
            buildExpressionFromGivenStructure(packet, struct, 0, new StringBuilder());
            result = packet.isEOF() == 1;
        }
        catch (Exception e) {
            result = false;
        }

        packet.setReadIndex(indexbuffer);
        return result;
    }

    public static void main(String[] args) throws InvalidPacketException {
        HPacket fghdft = fromString("{l}{h:-1}{i:1}{i:0}{i:6}{i:4}{s:\"1.0\"}");
        System.out.println(fghdft);

        HPacket zed = fromString("{out:test}{s:\"¥\"}{i:0}{i:0}");
        System.out.println(zed);

        HPacket p1 = fromString("{l}{h:1129}{s:\"\\\\\\\\\"}{i:0}{i:0}");
        System.out.println(p1.toExpression());
        HPacket p1_2 = fromString(p1.toExpression());
        System.out.println(p1_2.toExpression());

        HPacket p2 = fromString("{l}{h:4564}{i:3}{i:0}{s:\"hi\"}{i:0}{i:1}{s:\"how\"}{i:3}{b:1}{b:2}{b:3}{i:2}{s:\"r u\"}{i:1}{b:120}{i:2}{b:true}");
        System.out.println(p2);
        System.out.println(p2.toExpression());

        System.out.println(new HPacket("{l}{h:4564}{b:180}").toExpression());

        System.out.println(structureEquals(
                new HPacket("{l}{h:5}{s:\"asdas\"}"),
                "s"
        ));

        HPacket p3 = fromString("{l}{h:2266}{s:\"¥\"}{i:0}{i:0}");
        System.out.println(p3);
    }

}
