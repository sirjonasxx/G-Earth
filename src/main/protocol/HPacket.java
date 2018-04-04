package main.protocol;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HPacket {
    // te komen: toExpressions (+impl. expressies)
    private boolean isEdited = false;
    private byte[] packetInBytes;
    private int readIndex = 6;

    public HPacket(byte[] packet)	{
        packetInBytes = packet.clone();
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

    public byte[] toBytes()		{
        return packetInBytes;
    }

    public int getReadIndex()	{
        return readIndex;
    }
    public void setReadIndex(int number)	{
        readIndex = number;
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
        int length = readUshort();
        byte[] x = new byte[length];
        for (int i = 0; i < x.length; i++)	x[i] = readByte();

        try {
            return new String(x, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {	}

        return null;
    }
    public String readString(int index)	{
        int length = readUshort(index);
        index+=2;
        byte[] x = new byte[length];
        for (int i = 0; i < x.length; i++)	{ x[i] = readByte(index); index++;	}

        try {
            return new String(x, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {	}

        return null;
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

    //returns if done r not
    public boolean replaceFirstString(String oldS, String newS) {
        int i = 6;
        while (i < packetInBytes.length - 1 - oldS.length()) {
            if (readUshort(i) == oldS.length() && readString(i).equals(oldS)) {
                replaceString(i, newS);
                return true;
            }
            i++;
        }
        return false;
    }
    public HPacket replaceAllString(String oldS, String newS) {
        while (replaceFirstString(oldS, newS)) {}

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

    private void fixLength() {
        boolean remember = isEdited;
        replaceInt(0, packetInBytes.length - 4);
        isEdited = remember;
    }

    public static void main(String[] args) {
        HPacket packet = new HPacket("[0][0][0]4[15] [0]!PRODUCTION-201802201205-141713395[0][5]FLASH[0][0][0][1][0][0][0][0]");
        packet.replaceFirstString("FLASH", "HTML");
        System.out.println(packet);

    }

}