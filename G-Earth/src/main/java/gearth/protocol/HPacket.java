package gearth.protocol;

import gearth.misc.StringifyAble;
import gearth.misc.harble_api.HarbleAPI;
import gearth.misc.harble_api.HarbleAPIFetcher;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;

public class HPacket implements StringifyAble {

    private boolean isEdited = false;
    private byte[] packetInBytes;
    private int readIndex = 6;

    public HPacket(byte[] packet)	{
        packetInBytes = packet.clone();
    }
    public HPacket(HPacket packet) {
        packetInBytes = packet.packetInBytes.clone();
        isEdited = packet.isEdited;
    }
    public HPacket(String packet)	{
        packetInBytes = fromStringToBytes(fromExpressionToString(packet));
    }
    public HPacket(int header) {
        packetInBytes = new byte[]{0,0,0,2,0,0};
        replaceUShort(4, header);
        isEdited = false;
    }
    public HPacket(int header, byte[] bytes) {
        this(header);
        appendBytes(bytes);
        isEdited = false;
    }

    /**
     *
     * @param header headerId
     * @param objects can be a byte, integer, boolean, string, no short values allowed (use 2 bytes instead)
     */
    public HPacket(int header, Object... objects) throws InvalidParameterException {
        this(header);
        for (int i = 0; i < objects.length; i++) {
            Object o = objects[i];
            if (o instanceof Byte) {
                appendByte((Byte)o);
            }
            else if (o instanceof Integer) {
                appendInt((Integer)o);
            }
            else if (o instanceof String) {
                appendString((String)o);
            }
            else if (o instanceof Boolean) {
                appendBoolean((Boolean) o);
            }
            else {
                throw new InvalidParameterException();
            }
        }

        isEdited = false;
    }

    public String toString()	{
        String teststring = "";
        for (byte x : packetInBytes)	{
            if ((x < 32 && x >= 0) || x < -96 || x == 93 || x == 91 || x == 125 || x == 123 || x == 127 )
                teststring+="["+((((int)x) + 256 ) % 256)+"]";
            else
                try {
                    teststring+=new String(new byte[]{x}, "ISO-8859-1"); //"ISO-8859-1"
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
        }
        return teststring;
    }

    /**
     * returns "" if invalid expression, replaces all {} occurences except {l}
     * @return
     */
    private static String fromExpressionToString(String input) {
        try {
            int i = 0;
            char[] asChararray = input.toCharArray();
            StringBuilder newString = new StringBuilder();
            if (input.startsWith("{l}")) {
                newString.append("{l}");
                i = 3;
            }

            while (i < input.length()) {
                if (asChararray[i] != '{') {
                    newString.append(asChararray[i]);
                    i++;
                }
                else {
                    i++;
                    StringBuilder typeBuilder = new StringBuilder();
                    while (i < input.length() && asChararray[i] != ':') {
                        typeBuilder.append(asChararray[i]);
                        i++;
                    }
                    if (i == input.length()) throw new Exception();
                    String type = typeBuilder.toString();
                    i++;

                    StringBuilder inhoudBuilder = new StringBuilder();
                    while (i < input.length() && asChararray[i] != '}') {
                        inhoudBuilder.append(asChararray[i]);
                        i++;
                    }
                    if (i == input.length()) throw new Exception();
                    String inhoud = inhoudBuilder.toString();
                    i++;

                    if (type.equals("u")) {
                        int probs = Integer.parseInt(inhoud);
                        if (probs < 0 || probs >= 256 * 256) throw new Exception();
                        newString.append("[").append(probs / 256).append("][").append(probs % 256).append("]");
                    }
                    else if (type.equals("i")) {
                        ByteBuffer b = ByteBuffer.allocate(4).putInt(Integer.parseInt(inhoud));
                        newString.append(new HPacket(b.array()).toString());
                    }
                    else if (type.equals("d")) {
                        ByteBuffer b = ByteBuffer.allocate(8).putDouble(Double.parseDouble(inhoud));
                        newString.append(new HPacket(b.array()).toString());
                    }
                    else if (type.equals("b")) { // could be a byte or a boolean, no one cares
                        if (inhoud.toLowerCase().equals("true") || inhoud.toLowerCase().equals("false")) {
                            newString.append(inhoud.toLowerCase().equals("true") ? "[1]" : "[0]");
                        }
                        else {
                            int probs = Integer.parseInt(inhoud);
                            if (probs < 0 || probs >= 256) throw new Exception();
                            newString.append("[").append(probs).append("]");
                        }
                    }
                    else if (type.equals("s")) {
                        int probs = inhoud.length();
                        if (probs < 0 || probs >= 256 * 256) throw new Exception();
                        newString.append("[").append(probs / 256).append("][").append(probs % 256).append("]");

                        byte[] bts = inhoud.getBytes(StandardCharsets.ISO_8859_1);
                        for (int j = 0; j < inhoud.length(); j++) {
                            newString.append("[").append((((int)(bts[j])) + 256) % 256).append("]");
                        }
                    }
                    else throw new Exception();

                }
            }
            return newString.toString();
        }
        catch( Exception e) {
            return "";
        }
    }
    private static byte[] fromStringToBytes(String curstring)	{
        try	{
            ArrayList<Byte> bytes = new ArrayList<>();
            int index = 0;
            char[] asChararray = curstring.toCharArray();
            byte[] asliteralbytes = curstring.getBytes("ISO-8859-1");

            boolean startWithLength = false;
            if (curstring.startsWith("{l}")) {
                startWithLength = true;
                index = 3;
            }

            while (index < curstring.length()) {
                if (asChararray[index] == '[') {
                    int l = 2;
                    while (index + l < curstring.length() && asChararray[index + l] != ']') l++;
                    if (index + l == curstring.length()) throw new Exception();

                    int result = Integer.parseInt(curstring.substring(index + 1, index + l));
                    if (result > 255 || result < 0) throw new Exception();

                    byte rl = result > 127 ? (byte)(result - 256) : (byte)result ;

                    bytes.add(rl);
                    index = index + 1 + l;
                }
                else {
                    bytes.add(asliteralbytes[index]);
                    index++;
                }
            }

            byte[] result;
            if (startWithLength) {
                result = new byte[bytes.size() + 4];

                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(bytes.size());
                for (int i = 0; i < 4; i++) {
                    result[i] = b.array()[i];
                }
                for (int i = 0; i < bytes.size(); i++) {
                    result[i + 4] = bytes.get(i);
                }
            }
            else {
                result = new byte[bytes.size()];
                for (int i = 0; i < bytes.size(); i++) {
                    result[i] = bytes.get(i);
                }
            }

            return result;
        }
        catch (Exception e){}
        return new byte[0];
    }

    public boolean structureEquals(String structure) {
        if (isCorrupted()) return false;

        int indexbuffer = readIndex;
        readIndex = 6;

        String[] split = structure.split(",");

        for (int i = 0; i < split.length; i++) {
            String s = split[i];

            if (s.equals("s")) {
                if (readIndex + 2 > getBytesLength() || readUshort(readIndex) + 2 + readIndex > getBytesLength()) return false;
                readString();
            }
            else if (s.equals("i")) {
                if (readIndex + 4 > getBytesLength()) return false;
                readInteger();
            }
            else if (s.equals("u")) {
                if (readIndex + 2 > getBytesLength()) return false;
                readUshort();
            }
            else if (s.equals("b")) {
                if (readIndex + 1 > getBytesLength()) return false;
                readBoolean();
            }
        }

        boolean result = (isEOF() == 1);
        readIndex = indexbuffer;
        return result;
    }

    public int isEOF() {
        if (readIndex < getBytesLength()) return 0;
        if (readIndex == getBytesLength()) return 1;
        return 2;
    }

    public byte[] toBytes()		{
        return packetInBytes;
    }

    public int getReadIndex()	{
        return readIndex;
    }
    public void setReadIndex(int number)	{
        readIndex = number;
    }
    public void resetReadIndex() {
        setReadIndex(6);
    }

    public boolean isCorrupted()	{

        if (packetInBytes.length >= 6)	{
            if (length() == getBytesLength() - 4)	{
                return false;
            }
        }
        return true;
    }

    public byte readByte()	{
        readIndex++;
        return packetInBytes[readIndex - 1];
    }
    public byte readByte(int index)	{
        return packetInBytes[index];
    }

    public short readShort()	{
        byte[] btarray = new byte[]{packetInBytes[readIndex], packetInBytes[readIndex + 1]};
        readIndex +=2;
        return java.nio.ByteBuffer.wrap(btarray).getShort();
    }
    public short readShort(int index)	{
        byte[] btarray = new byte[]{packetInBytes[index], packetInBytes[index + 1]};
        return java.nio.ByteBuffer.wrap(btarray).getShort();
    }
    public int readUshort() {
        byte[] btarray = new byte[]{0, 0, packetInBytes[readIndex], packetInBytes[readIndex + 1]};
        readIndex +=2;
        return java.nio.ByteBuffer.wrap(btarray).getInt();
    }
    public int readUshort(int index) {
        byte[] btarray = new byte[]{0, 0, packetInBytes[index], packetInBytes[index + 1]};
        return java.nio.ByteBuffer.wrap(btarray).getInt();
    }

    public int headerId()	{
        return readUshort(4);
    }

    public int readInteger(){
        byte[] btarray = new byte[]{packetInBytes[readIndex], packetInBytes[readIndex + 1], packetInBytes[readIndex + 2], packetInBytes[readIndex + 3]};
        readIndex +=4;
        return java.nio.ByteBuffer.wrap(btarray).getInt();
    }
    public int readInteger(int index)	{
        byte[] btarray = new byte[]{packetInBytes[index], packetInBytes[index + 1], packetInBytes[index + 2], packetInBytes[index + 3]};
        return java.nio.ByteBuffer.wrap(btarray).getInt();
    }

    public double readDouble(){
        double result = readDouble(readIndex);
        readIndex += 8;
        return result;
    }
    public double readDouble(int index)	{
        return java.nio.ByteBuffer.wrap(packetInBytes).getDouble(index);
    }

    public int length()	{
        return readInteger(0);
    }
    public int getBytesLength()	{
        return packetInBytes.length;
    }

    public byte[] readBytes(int length)	{
        byte[] newbytes = new byte[length];
        for (int i = 0; i < (length); i++)	{
            newbytes[i] = packetInBytes[i+ readIndex];
        }
        readIndex +=length;
        return newbytes;
    }
    public byte[] readBytes(int length, int index)	{
        byte[] newbytes = new byte[length];
        for (int i = 0; i < (length); i++)	{
            newbytes[i] = packetInBytes[i+index];
        }
        return newbytes;
    }

    public long readLong()	{
        byte[] btarray = readBytes(8);
        return java.nio.ByteBuffer.wrap(btarray).getLong();
    }
    public long readLong(int index)	{
        byte[] btarray = readBytes(8, index);
        return java.nio.ByteBuffer.wrap(btarray).getLong();
    }

    public String readString()	{
        String r = readString(readIndex);
        readIndex += (2 + r.length());
        return r;
    }
    public String readString(int index)	{
        int length = readUshort(index);
        index+=2;

        return readString(index, length);
    }

    private String readString(int index, int length) {
        byte[] x = new byte[length];
        for (int i = 0; i < x.length; i++)	{ x[i] = readByte(index); index++;	}
        try {
            return new String(x, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {	}

        return null;
    }

    public String readLongString()	{
        String r = readLongString(readIndex);
        readIndex += (4 + r.length());
        return r;
    }
    public String readLongString(int index) {
        int length = readInteger(index);
        index += 4;

        return readString(index, length);
    }

    public boolean readBoolean()	{
        return (readByte() != 0);
    }
    public boolean readBoolean(int index)	{
        return (readByte(index) != 0);
    }


    public HPacket replaceBoolean(int index, boolean b) {
        isEdited = true;
        packetInBytes[index] = b ? (byte)1 : (byte)0;
        return this;
    }
    public HPacket replaceInt(int index, int i) {
        isEdited = true;
        ByteBuffer b = ByteBuffer.allocate(4).putInt(i);
        for (int j = 0; j < 4; j++) {
            packetInBytes[index + j] = b.array()[j];
        }
        return this;
    }
    public HPacket replaceDouble(int index, double d) {
        isEdited = true;
        ByteBuffer b = ByteBuffer.allocate(8).putDouble(d);
        for (int j = 0; j < 8; j++) {
            packetInBytes[index + j] = b.array()[j];
        }
        return this;
    }
    public HPacket replaceByte(int index, byte b) {
        isEdited = true;
        packetInBytes[index] = b;
        return this;
    }
    public HPacket replaceBytes(int index, byte[] bytes) {
        isEdited = true;
        int i = 0;
        while (index + i < packetInBytes.length && i < bytes.length) {
            replaceByte(index + i, bytes[i]);
            i++;
        }
//
//        if (i < bytes.length) {
//            appendBytes(Arrays.copyOfRange(bytes, i, bytes.length));
//            fixLength();
//        }
        return this;
    }
    public HPacket replaceUShort(int index, int ushort) {
        isEdited = true;
        ByteBuffer b = ByteBuffer.allocate(4).putInt(ushort);
        packetInBytes[index] = b.array()[2];
        packetInBytes[index + 1] = b.array()[3];
        return this;
    }
    public HPacket replaceShort(int index, short s) {
        isEdited = true;
        ByteBuffer b = ByteBuffer.allocate(2).putShort(s);
        packetInBytes[index] = b.array()[0];
        packetInBytes[index + 1] = b.array()[1];
        return this;
    }
    public HPacket replaceString(int index, String s) {
        isEdited = true;
        byte[] sbytes = s.getBytes(StandardCharsets.ISO_8859_1);
        int mover = s.length() - readUshort(index);

        if (mover != 0) {
            byte[] newPacket = Arrays.copyOf(packetInBytes, packetInBytes.length + mover);

            if (mover > 0) {
                int i = newPacket.length - 1;
                while (i > index + mover + 2) {
                    newPacket[i] = packetInBytes[i - mover];
                    i--;
                }
            }
            else {
                int i = index + 2 + s.length();
                while (i < newPacket.length) {
                    newPacket[i] = packetInBytes[i - mover];
                    i++;
                }
            }

            packetInBytes = newPacket;
            fixLength();
        }

        replaceUShort(index, s.length());
        for (int i = 0; i < s.length(); i++) {
            packetInBytes[index + 2 + i] = sbytes[i];
        }
        return this;
    }

    private boolean canReadString(int index) {
        if (index < packetInBytes.length - 1) {
            int l = readUshort(index);
            if (index + 1 + l < packetInBytes.length) {
                return true;
            }
        }
        return false;
    }

    //returns if done r not
    public HPacket replaceFirstString(String oldS, String newS) {
        return replaceXStrings(oldS, newS, 1);
    }
    public HPacket replaceXStrings(String oldS, String newS, int amount) {
        if (amount == 0) return this;

        int i = 6;
        while (i < packetInBytes.length - 1 - oldS.length()) {
            if (readUshort(i) == oldS.length() && readString(i).equals(oldS)) {
                replaceString(i, newS);
                i += 1 + newS.length();
                amount -= 1;
                if (amount == 0) {
                    return this;
                }
            }
            i++;
        }
        return this;
    }
    public HPacket replaceAllStrings(String oldS, String newS) {
        return replaceXStrings(oldS, newS, -1);
    }

    public HPacket replaceFirstSubstring(String oldS, String newS) {
        return replaceXSubstrings(oldS, newS, 1);
    }
    public HPacket replaceXSubstrings(String oldS, String newS, int amount) {
        if (amount == 0) {
            return this;
        }

        int max = packetInBytes.length;
        int i = packetInBytes.length - 2 - oldS.length();
        while (i >= 6) {
            if (canReadString(i)) {
                String s = readString(i);
                System.out.println(s.contains(oldS));
                if (s.contains(oldS) && i + 2 + s.length() <= max) {
                    String replacement = s.replaceAll(oldS, newS);

                    replaceString(i, replacement);
                    i -= (1 + oldS.length());
                    amount -= 1;
                    if (amount == 0) {
                        return this;
                    }

                    max = i;
                }
            }
            i--;
        }
        return this;
    }
    public HPacket replaceAllSubstrings(String oldS, String newS) {
        return replaceXSubstrings(oldS, newS, -1);
    }

    public HPacket replaceAllIntegers(int val, int replacement) {
        int i = 6;
        while (i < packetInBytes.length - 3) {
            if (readInteger(i) == val) {
                replaceInt(i, replacement);
                i += 3;
            }
            i++;
        }
        return this;
    }


    public HPacket appendInt(int i) {
        isEdited = true;
        packetInBytes = Arrays.copyOf(packetInBytes, packetInBytes.length + 4);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4).putInt(i);
        for (int j = 0; j < 4; j++) {
            packetInBytes[packetInBytes.length - 4 + j] = byteBuffer.array()[j];
        }
        fixLength();
        return this;
    }
    public HPacket appendDouble(double d) {
        isEdited = true;
        packetInBytes = Arrays.copyOf(packetInBytes, packetInBytes.length + 8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(8).putDouble(d);
        for (int j = 0; j < 8; j++) {
            packetInBytes[packetInBytes.length - 8 + j] = byteBuffer.array()[j];
        }
        fixLength();
        return this;
    }
    public HPacket appendByte(byte b) {
        isEdited = true;
        packetInBytes = Arrays.copyOf(packetInBytes, packetInBytes.length + 1);
        packetInBytes[packetInBytes.length - 1] = b;
        fixLength();
        return this;
    }
    public HPacket appendBytes(byte[] bytes) {
        isEdited = true;
        packetInBytes = Arrays.copyOf(packetInBytes, packetInBytes.length + bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            packetInBytes[packetInBytes.length - bytes.length + i] = bytes[i];
        }
        fixLength();
        return this;
    }
    public HPacket appendBoolean(boolean b) {
        isEdited = true;
        appendByte((byte)(b ? 1 : 0));
        return this;
    }
    public HPacket appendUShort(int ushort) {
        isEdited = true;
        packetInBytes = Arrays.copyOf(packetInBytes, packetInBytes.length + 2);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4).putInt(ushort);
        for (int j = 2; j < 4; j++) {
            packetInBytes[packetInBytes.length - 4 + j] = byteBuffer.array()[j];
        }
        fixLength();
        return this;
    }
    public HPacket appendShort(short s) {
        isEdited = true;
        packetInBytes = Arrays.copyOf(packetInBytes, packetInBytes.length + 2);
        ByteBuffer byteBuffer = ByteBuffer.allocate(2).putShort(s);
        for (int j = 0; j < 2; j++) {
            packetInBytes[packetInBytes.length - 2 + j] = byteBuffer.array()[j];
        }
        fixLength();
        return this;
    }
    public HPacket appendString(String s) {
        isEdited = true;
        appendUShort(s.length());
        appendBytes(s.getBytes(StandardCharsets.ISO_8859_1));
        return this;
    }
    public HPacket appendLongString(String s) {
        isEdited = true;
        appendInt(s.length());
        appendBytes(s.getBytes(StandardCharsets.ISO_8859_1));
        return this;
    }

    public HPacket removeFrom(int index) {
        return removeRange(index, packetInBytes.length - index);
    }
    public HPacket removeRange(int index, int length) {
        isEdited = true;
        for (int i = index; i < packetInBytes.length - length; i++) {
            packetInBytes[i] = packetInBytes[i + length];
        }
        packetInBytes = Arrays.copyOf(packetInBytes, packetInBytes.length - length);
        fixLength();
        return this;
    }

    public boolean isReplaced() {
        return isEdited;
    }

    public void fixLength() {
        boolean remember = isEdited;
        replaceInt(0, packetInBytes.length - 4);
        isEdited = remember;
    }

    public void overrideEditedField(boolean edited) {
        isEdited = edited;
    }


    private String toExpressionFromGivenStructure(String struct) {
        int oldReadIndex = readIndex;
        resetReadIndex();

        StringBuilder builder = new StringBuilder();
        builder.append("{l}{u:").append(headerId()).append("}");

        buildExpressionFromGivenStructure(struct, 0, builder);
        readIndex = oldReadIndex;
        return builder.toString();
    }

    private void buildExpressionFromGivenStructure(String struct, int indexInGivenStruct, StringBuilder builder) {
        int prevInt = 0;

        while (indexInGivenStruct < struct.length()) {
            char c = struct.charAt(indexInGivenStruct++);
            if (c == '(') {
                for (int i = 0; i < prevInt; i++) buildExpressionFromGivenStructure(struct, indexInGivenStruct, builder);
                int skipping = 1;
                while (skipping > 0) {
                    char c2 = struct.charAt(indexInGivenStruct++);
                    if (c2 == '(') skipping++;
                    else if (c2 == ')') skipping--;
                }
            }
            else if (c == 'i') builder.append("{i:").append(prevInt = readInteger()).append('}');
            else if (c == 's') builder.append("{s:").append(readString()).append('}');
            else if (c == 'd') builder.append("{d:").append(readDouble()).append('}');
            else if (c == 'b') builder.append("{b:").append(readByte()).append('}');
            else if (c == 'B') builder.append("{b:").append(readBoolean()).append('}');
            else return; // ')'
        }
    }

    public String toExpression(HMessage.Side side) {
        if (isCorrupted()) return "";

        HarbleAPI.HarbleMessage msg;
        if (HarbleAPIFetcher.HARBLEAPI != null &&
                ((msg = HarbleAPIFetcher.HARBLEAPI.getHarbleMessageFromHeaderId(side, headerId())) != null)) {
            if (msg.getStructure() != null) {
                return toExpressionFromGivenStructure(msg.getStructure());
            }
        }
        return toExpression();
    }

    /**
     * returns "" if not found or not sure enough
     * dont hate on the coding quality in this function, its pretty effective.
     */
    public String toExpression() {
        if (isCorrupted()) return "";

        boolean[] mask = new boolean[packetInBytes.length];
        String[] resultTest = new String[packetInBytes.length];

        for (int i = 0; i < 6; i++) {
            mask[i] = true;
        }

        resultTest[0] = "{l}";
        resultTest[4] = "{u:"+headerId()+"}";

        outerloop:
        for (int i = 6; i < packetInBytes.length - 1; i++) {
            int potentialstringlength = readUshort(i);
            if ((potentialstringlength >= 0 && potentialstringlength < 3) || potentialstringlength > packetInBytes.length - i - 2) continue;

            for (int j = i; j < potentialstringlength+i+2; j++) {
                if (mask[j]) continue outerloop;
            }

            for (int j = i+2; j < potentialstringlength+i+2; j++) {
                if (readByte(j) >= 0 && readByte(j) < 6) continue  outerloop;
            }

            if (i + 2 + potentialstringlength >= packetInBytes.length - 3 ||
                    (packetInBytes[i+2+potentialstringlength] >= 0 &&
                            packetInBytes[i+2+potentialstringlength] < 6 )) {

                for (int j = i; j < potentialstringlength+i+2; j++) {
                    mask[j] = true;
                }
                resultTest[i] = "{s:"+readString(i)+"}";
                i += (1 + potentialstringlength);
            }
        }

        //TODO add special case for seperated 5, 6 and 7 bytes here
        // long live the shitty code.

        //5
        out:
        for (int i = 6; i < packetInBytes.length - 4; i++) {
            for (int j = i; j < i+5; j++) {
                if (mask[j]) {
                    i = j;
                    continue out;
                }
            }
            if (!mask[i-1] || (i+5 < packetInBytes.length && !mask[i+5])) continue;

            if ((readByte(i) == 0 || readByte(i) == 1) && (readInteger(i+1) > 1 || readInteger(i+1) < 0)) {
                //decide the first byte to be the a boolean
                resultTest[i] = "{b:"+(readBoolean(i) ? "true" : "false")+"}";
                resultTest[i+1] = "{i:"+readInteger(i+1)+"}";
                for (int j = i; j < i+5; j++) {
                    mask[j] = true;
                }
            }

        }

//        //6
//        out:
//        for (int i = 6; i < packetInBytes.length - 5; i++) {
//            for (int j = i; j < i+6; j++) {
//                if (mask[j]) {
//                    i = j;
//                    continue out;
//                }
//            }
//            if (i+6 < packetInBytes.length && !mask[i+6]) continue;
//
//
//
//        }
//
//        //7
//        out:
//        for (int i = 6; i < packetInBytes.length - 6; i++) {
//            for (int j = i; j < i+7; j++) {
//                if (mask[j]) {
//                    i = j;
//                    continue out;
//                }
//            }
//            if (i+7 < packetInBytes.length && !mask[i+7]) continue;
//
//
//
//        }

        lp22:
        for (int i = 6; i < packetInBytes.length - 3; i++) {
            for (int j = i; j < i + 4; j++) {
                if (mask[j]) {
                    continue lp22;
                }
            }

            int num = readInteger(i);
            if (num == -1 || (num >= 0 && num < 256)) {
                for (int j = i; j < i+4; j++) {
                    mask[j] = true;
                }
                resultTest[i] = "{i:"+num+"}";
                i += 3;
            }
        }


        boolean changed = true;
        boolean isfirst = true;

        while (changed) {
            changed = false;
            // filtering strings
            outerloop:
            for (int i = 6; i < packetInBytes.length - 1; i++) {
                int potentialstringlength = readUshort(i);
                if ((potentialstringlength >= 0 && potentialstringlength < 3) || potentialstringlength > packetInBytes.length - i - 2) continue;

                for (int j = i; j < potentialstringlength+i+2; j++) {
                    if (mask[j]) continue outerloop;
                }

                for (int j = i+2; j < potentialstringlength+i+2; j++) {
                    if (readByte(j) >= 0 && readByte(j) < 6) continue  outerloop;
                }

                if (i + 2 + potentialstringlength >= packetInBytes.length - 3 ||
                        (packetInBytes[i+2+potentialstringlength] >= 0 &&
                                packetInBytes[i+2+potentialstringlength] < 6 )) {

                    for (int j = i; j < potentialstringlength+i+2; j++) {
                        mask[j] = true;
                    }
                    changed = true;
                    resultTest[i] = "{s:"+readString(i)+"}";
                    i += (1 + potentialstringlength);
                }
            }

            if (isfirst) {
                int count = 0;
                for (int i = 6; i < packetInBytes.length; i++) {
                    if (!mask[i]) count++;
                }
                if (count > 300) return "";
            }
            isfirst = false;

            // filtering integers
            boolean hasfoundsomtin = true;
            while (hasfoundsomtin) {
                hasfoundsomtin = false;
                outerloop2:
                for (int i = 6; i < packetInBytes.length - 3; i++) {
                    for (int j = i; j < i + 4; j++) {
                        if (mask[j]) {
                            continue outerloop2;
                        }
                    }

                    if (i + 4 == packetInBytes.length || mask[i+4] || mask[i-1]) {
                        if (packetInBytes[i+1] == 2) { //could be an unfiltered string; don't filter yet
                            if (((packetInBytes[i+2] >= '0' && packetInBytes[i+2] <= '9') ||
                                    (packetInBytes[i+2] >= 'a' && packetInBytes[i+2] <= 'z') ||
                                    (packetInBytes[i+2] >= 'A' && packetInBytes[i+2] <= 'Z')) &&
                                    ((packetInBytes[i+3] >= '0' && packetInBytes[i+3] <= '9') ||
                                            (packetInBytes[i+3] >= 'a' && packetInBytes[i+3] <= 'z') ||
                                            (packetInBytes[i+3] >= 'A' && packetInBytes[i+3] <= 'Z'))) {
                                changed = true;
                                for (int j = i; j < i + 4; j++) {
                                    mask[j] = true;
                                }
                                hasfoundsomtin = true;
                                resultTest[i] = "{i:"+readInteger(i)+"}";
                                continue;
                            }
                            continue ;
                        }
                        else {
                            for (int j = i; j < i + 4; j++) {
                                mask[j] = true;
                            }
                            hasfoundsomtin = true;
                            changed = true;
                            resultTest[i] = "{i:"+readInteger(i)+"}";
                            continue;
                        }
                    }
                    if (readInteger(i) < 65536) {
                        for (int j = i; j < i + 4; j++) {
                            mask[j] = true;
                        }
                        hasfoundsomtin = true;
                        resultTest[i] = "{i:"+readInteger(i)+"}";
                        changed = true;
                        continue;
                    }

                }
            }


            // filtering strings

            outerloop3:
            for (int i = 6; i < packetInBytes.length - 1; i++) {
                int potentialstringlength = readUshort(i);
                if (potentialstringlength > packetInBytes.length - i - 2) continue;

                for (int j = i; j < potentialstringlength+i+2; j++) {
                    if (mask[j]) continue outerloop3;
                }

                for (int j = i+2; j < potentialstringlength+i+2; j++) {
                    if (readByte(j) >= 0 && readByte(j) < 6) continue outerloop3;
                }

                for (int j = i; j < potentialstringlength+i+2; j++) {
                    mask[j] = true;
                }
                resultTest[i] = "{s:"+readString(i)+"}";
                i += (1 + potentialstringlength);
                changed = true;
            }
        }

        int opeenvolging = 0;
        for (int i = 6; i < packetInBytes.length; i++) {
            if (!mask[i]) {
                opeenvolging++;
                if (opeenvolging == 4) return "";
                if (i+1 == packetInBytes.length || mask[i+1]) {
                    for (int j = i - opeenvolging + 1; j <= i; j++) {
                        mask[j] = true;
                        if (packetInBytes[j] == 1 || packetInBytes[j] == 0) {
                            resultTest[j] = "{b:"+(packetInBytes[j] == 1 ? "true" : "false")+"}";
                        }
                        else {
                            resultTest[j] = "{b:"+((((int)packetInBytes[j])+256)%256)+"}";
                        }
                    }
                    opeenvolging = 0;


                }
            }
        }


        // if all values in mask are true, go further
        for (boolean bool : mask) {
            if (!bool) return "";
        }

        StringBuilder expression = new StringBuilder();
        for (int i = 0; i < resultTest.length; i++) {
            if (resultTest[i] != null) expression.append(resultTest[i]);
        }

        return expression.toString().replace("{i:0}{b:false}{b:true}", "{s:}{i:1}");
    }

    @Override
    public String stringify() {
        String st = null;
        st = (isEdited ? "1" : "0") + new String(packetInBytes, StandardCharsets.ISO_8859_1);
        return st;
    }

    @Override
    public void constructFromString(String str) {
        this.isEdited = str.charAt(0) == '1';
        packetInBytes = str.substring(1).getBytes(StandardCharsets.ISO_8859_1);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof HPacket)) return false;

        HPacket packet2 = (HPacket) object;
        return Arrays.equals(packetInBytes, packet2.packetInBytes) && (isEdited == packet2.isEdited);
    }

    public static void main(String[] args) {
        HPacket packet = new HPacket("{l}{u:4564}{i:3}{i:0}{s:hi}{i:0}{i:1}{s:how}{i:3}{b:1}{b:2}{b:3}{i:2}{s:r u}{i:1}{b:120}{i:2}{b:true}");

        String str = packet.toExpressionFromGivenStructure("i(isi(b))iB");

        HPacket packetverify = new HPacket(str);

        System.out.println(str);
        System.out.println(packetverify.toString().equals(packet.toString()));

    }
}