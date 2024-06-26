package gearth.protocol.packethandler.shockwave.buffers;

import gearth.encoding.Base64Encoding;
import gearth.protocol.crypto.RC4Cipher;
import gearth.protocol.packethandler.PayloadBuffer;

import java.util.ArrayList;
import java.util.Arrays;

public class ShockwaveOutBuffer extends PayloadBuffer {

    public static final int PACKET_HEADER_SIZE = 2;

    public static final int PACKET_LENGTH_SIZE_ENCRYPTED = 6;
    public static final int PACKET_LENGTH_SIZE = 3;

    public static final int PACKET_SIZE_MIN = PACKET_HEADER_SIZE + PACKET_LENGTH_SIZE;
    public static final int PACKET_SIZE_MIN_ENCRYPTED = PACKET_HEADER_SIZE + PACKET_LENGTH_SIZE_ENCRYPTED;

    private RC4Cipher cipher;

    @Override
    public void setCipher(RC4Cipher cipher) {
        this.cipher = cipher.deepCopy();
    }

    @Override
    public byte[][] receive() {
        final int packetLengthSize = this.cipher != null ? PACKET_LENGTH_SIZE_ENCRYPTED : PACKET_LENGTH_SIZE;
        final int minPacketSize = this.cipher != null ? PACKET_SIZE_MIN_ENCRYPTED : PACKET_SIZE_MIN;

        if (buffer.length < minPacketSize) {
            return new byte[0][];
        }

        final ArrayList<byte[]> out = new ArrayList<>();

        while (buffer.length >= minPacketSize) {
            int length;

            if (this.cipher != null) {
                final byte[] decData = this.cipher.decipher(buffer, 0, PACKET_LENGTH_SIZE_ENCRYPTED);
                final int decDataLen = Base64Encoding.decode(new byte[]{decData[1], decData[2], decData[3]});

                // TODO: Store length in a variable for if we don't have enough bytes.

                length = decDataLen;
            } else {
                length = Base64Encoding.decode(new byte[]{buffer[0], buffer[1], buffer[2]});
            }

            if (buffer.length < length + packetLengthSize) {
                break;
            }

            int endPos = length + packetLengthSize;

            out.add(Arrays.copyOfRange(buffer, packetLengthSize, endPos));

            buffer = Arrays.copyOfRange(buffer, endPos, buffer.length);
        }

        return out.toArray(new byte[0][]);
    }
}
