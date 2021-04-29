package gearth.protocol;

import gearth.extensions.parsers.HDirection;
import gearth.misc.StringifyAble;
import gearth.services.packet_info.PacketInfo;
import gearth.services.packet_info.PacketInfoManager;
import gearth.services.packet_representation.InvalidPacketException;
import gearth.services.packet_representation.PacketStringUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Optional;

public class HPacket implements StringifyAble {

    private boolean isEdited = false;
    private byte[] packetInBytes;
    private int readIndex = 6;

    // if identifier != null, this is a placeholder name for the type of packet, headerId will be "-1"
    private String identifier = null;
    private HMessage.Direction identifierDirection = null;

    public HPacket(byte[] packet)	{
        packetInBytes = packet.clone();
    }
    public HPacket(HPacket packet) {
        packetInBytes = packet.packetInBytes.clone();
        isEdited = packet.isEdited;
    }
    public HPacket(String packet)	{
        try {
            HPacket packetFromString = PacketStringUtils.fromString(packet);
            packetInBytes = packetFromString.packetInBytes;
            identifier = packetFromString.identifier;
            identifierDirection = packetFromString.identifierDirection;
    } catch (InvalidPacketException e) {
        packetInBytes = new byte[0];
    }
}
    public HPacket(int header) {
        packetInBytes = new byte[]{0,0,0,2,0,0};
        replaceShort(4, (short)header);
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
        appendObjects(objects);
        isEdited = false;
    }

    public HPacket(String identifier, HMessage.Direction direction) throws InvalidParameterException {
        packetInBytes = new byte[]{0,0,0,2,-1,-1};
        this.identifier = identifier;
        this.identifierDirection = direction;
    }

    public HPacket(String identifier, HMessage.Direction direction, Object... objects) throws InvalidParameterException {
        this(identifier, direction);
        appendObjects(objects);
        isEdited = false;
    }



    public String toString()	{
        return PacketStringUtils.toString(packetInBytes);
    }

    public boolean structureEquals(String structure) {
        return PacketStringUtils.structureEquals(this, structure);
    }

    public int isEOF() {
        if (readIndex < getBytesLength()) return 0;
        if (readIndex == getBytesLength()) return 1;
        return 2;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setIdentifierDirection(HMessage.Direction identifierDirection) {
        this.identifierDirection = identifierDirection;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void completePacket(PacketInfoManager packetInfoManager) {
        if (isCorrupted() || identifier == null) return;

        PacketInfo packetInfo = packetInfoManager.getPacketInfoFromName(identifierDirection, identifier);
        if (packetInfo == null) {
            packetInfo = packetInfoManager.getPacketInfoFromHash(identifierDirection, identifier);
            if (packetInfo == null) return;
        }

        boolean wasEdited = isEdited;
        replaceShort(4, (short)(packetInfo.getHeaderId()));
        identifier = null;

        isEdited = wasEdited;
    }

    public boolean canSendToClient() {
        return identifierDirection == null || identifierDirection == HMessage.Direction.TOCLIENT;
    }

    public boolean canSendToServer() {
        return identifierDirection == null || identifierDirection == HMessage.Direction.TOSERVER;
    }

    public boolean canComplete(PacketInfoManager packetInfoManager) {
        if (isCorrupted() || identifier == null) return false;

        PacketInfo packetInfo = packetInfoManager.getPacketInfoFromName(identifierDirection, identifier);
        if (packetInfo == null) {
            packetInfo = packetInfoManager.getPacketInfoFromHash(identifierDirection, identifier);
            return packetInfo != null;
        }

        return true;
    }

    public boolean isPacketComplete() {
        return identifier == null;
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
        return readShort(4);
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

    public float readFloat(){
        float result = readFloat(readIndex);
        readIndex += 4;
        return result;
    }
    public float readFloat(int index)	{
        return java.nio.ByteBuffer.wrap(packetInBytes).getFloat(index);
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


    public String readString(Charset charset)	{
        String r = readString(readIndex, charset);
        readIndex += (2 + readUshort(readIndex));
        return r;
    }
    public String readString(int index, Charset charset)	{
        int length = readUshort(index);
        index+=2;
        return readString(index, length, charset);
    }

    public String readString()	{
        return readString(StandardCharsets.ISO_8859_1);
    }
    public String readString(int index)	{
        return readString(index, StandardCharsets.ISO_8859_1);
    }

    private String readString(int index, int length, Charset charset) {
        byte[] x = new byte[length];
        for (int i = 0; i < x.length; i++)	{ x[i] = readByte(index); index++;	}
        return new String(x, charset);
    }


    public String readLongString(Charset charset)	{
        String r = readLongString(readIndex, charset);
        readIndex += (4 + readInteger(readIndex));
        return r;
    }
    public String readLongString(int index, Charset charset) {
        int length = readInteger(index);
        index += 4;

        return readString(index, length, charset);
    }

    public String readLongString()	{
        return readLongString(StandardCharsets.ISO_8859_1);
    }
    public String readLongString(int index)	{
        return readLongString(index, StandardCharsets.ISO_8859_1);
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
    public HPacket replaceLong(int index, long l) {
        isEdited = true;
        ByteBuffer b = ByteBuffer.allocate(8).putLong(l);
        for (int j = 0; j < 8; j++) {
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
    public HPacket replaceFloat(int index, float f) {
        isEdited = true;
        ByteBuffer b = ByteBuffer.allocate(4).putFloat(f);
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

    public HPacket replaceString(int index, String s, Charset charset) {
        isEdited = true;
        byte[] sbytes = s.getBytes(charset);
        int mover = sbytes.length - readUshort(index);

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
                int i = index + 2 + sbytes.length;
                while (i < newPacket.length) {
                    newPacket[i] = packetInBytes[i - mover];
                    i++;
                }
            }

            packetInBytes = newPacket;
            fixLength();
        }

        replaceUShort(index, sbytes.length);
        for (int i = 0; i < sbytes.length; i++) {
            packetInBytes[index + 2 + i] = sbytes[i];
        }
        return this;
    }

    public HPacket replaceString(int index, String s) {
        return replaceString(index, s, StandardCharsets.ISO_8859_1);
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
    public HPacket appendLong(long l) {
        isEdited = true;
        packetInBytes = Arrays.copyOf(packetInBytes, packetInBytes.length + 8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(8).putLong(l);
        for (int j = 0; j < 8; j++) {
            packetInBytes[packetInBytes.length - 8 + j] = byteBuffer.array()[j];
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
    public HPacket appendFloat(float f) {
        isEdited = true;
        packetInBytes = Arrays.copyOf(packetInBytes, packetInBytes.length + 4);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4).putFloat(f);
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
    public HPacket appendString(String s, Charset charset) {
        isEdited = true;
        appendUShort(s.getBytes(charset).length);
        appendBytes(s.getBytes(charset));
        return this;
    }
    public HPacket appendString(String s) {
        return appendString(s, StandardCharsets.ISO_8859_1);
    }

    public HPacket appendLongString(String s, Charset charset) {
        isEdited = true;
        appendInt(s.getBytes(charset).length);
        appendBytes(s.getBytes(charset));
        return this;
    }

    public HPacket appendLongString(String s) {
        return appendLongString(s, StandardCharsets.ISO_8859_1);
    }

    public HPacket appendObjects(Object... objects) {
        for (Object object : objects) {
            appendObject(object);
        }

        return this;
    }

    public HPacket appendObject(Object o) throws InvalidParameterException {
        isEdited = true;

        if (o instanceof Byte) {
            appendByte((Byte)o);
        }
        else if (o instanceof Integer) {
            appendInt((Integer)o);
        }
        else if (o instanceof Short) {
            appendShort((Short)o);
        }
        else if (o instanceof String) {
            appendString((String)o, StandardCharsets.UTF_8);
        }
        else if (o instanceof Boolean) {
            appendBoolean((Boolean) o);
        }
        else if (o instanceof Long) {
            appendLong((Long) o);
        }
        else if (o instanceof Float) {
            appendFloat((Float) o);
        }
        else if (o instanceof Double) {
            appendDouble((Double) o);
        }
        else {
            throw new InvalidParameterException();
        }

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

    private PacketInfo getPacketInfo(HMessage.Direction direction, PacketInfoManager packetInfoManager) {
        if (packetInfoManager == null) return null;

        // prefer packetinfo with structure information (not available in at time of writing)
        Optional<PacketInfo> packetInfoMaybe = packetInfoManager.getAllPacketInfoFromHeaderId(direction, headerId())
                .stream().filter(packetInfo -> packetInfo.getStructure() != null).findFirst();
        return packetInfoMaybe.orElseGet(() -> packetInfoManager.getPacketInfoFromHeaderId(direction, headerId()));
    }

    public String toExpression(HMessage.Direction direction, PacketInfoManager packetInfoManager, boolean removeShuffle) {
        if (isCorrupted()) return "";

        PacketInfo packetInfo = getPacketInfo(direction, packetInfoManager);
        if (packetInfo != null && packetInfo.getStructure() != null) {
            return PacketStringUtils.toExpressionFromGivenStructure(this, packetInfo.getStructure(), removeShuffle ? packetInfo : null);
        }
        return PacketStringUtils.predictedExpression(this, removeShuffle ? packetInfo : null);
    }

    /**
     * returns "" if not found or not sure enough
     */
    public String toExpression(PacketInfoManager packetInfoManager, boolean removeShuffle) {
        if (isCorrupted()) return "";

        PacketInfo maybe1 = getPacketInfo(HMessage.Direction.TOCLIENT, packetInfoManager);
        PacketInfo maybe2 = getPacketInfo(HMessage.Direction.TOSERVER, packetInfoManager);

        PacketInfo packetInfo = null;

        if (maybe1 != null && maybe2 == null) {
            packetInfo = maybe1;
        }
        else if (maybe1 == null && maybe2 != null) {
            packetInfo = maybe2;
        }

        if (packetInfo != null && packetInfo.getStructure() != null) {
            return PacketStringUtils.toExpressionFromGivenStructure(this, packetInfo.getStructure(), removeShuffle ? packetInfo : null);
        }
        return PacketStringUtils.predictedExpression(this, removeShuffle ? packetInfo : null);
    }

    public String toExpression() {
        if (isCorrupted()) return "";
        return PacketStringUtils.predictedExpression(this, null);
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
}
