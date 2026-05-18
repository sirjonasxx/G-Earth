package gearth.app.protocol.packethandler.shockwave.buffers;

import gearth.app.protocol.crypto.HeaderCipher;
import gearth.app.protocol.packethandler.PayloadBuffer;
import gearth.encoding.Base64Encoding;

import java.util.ArrayList;
import java.util.Arrays;

public class ShockwaveProperBuffer extends PayloadBuffer {

    public static final int PACKET_HEADER_SIZE = 2;

    public static final int PACKET_LENGTH_SIZE_ENCRYPTED = 6;
    public static final int PACKET_LENGTH_SIZE = 3;

    public static final int PACKET_SIZE_MIN = PACKET_HEADER_SIZE + PACKET_LENGTH_SIZE;
    public static final int PACKET_SIZE_MIN_ENCRYPTED = PACKET_HEADER_SIZE + PACKET_LENGTH_SIZE_ENCRYPTED;

    private int previousLength = 0;
    private HeaderCipher cipher;

    public void setCipher(HeaderCipher cipher) {
        this.cipher = cipher;
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
                if (previousLength  == 0) {
                    final byte[] decData = this.cipher.decipher(buffer, 0, PACKET_LENGTH_SIZE_ENCRYPTED);

                    if (decData.length < 4) {
                        throw new IllegalStateException("Decrypted packet length is less than 4 bytes.");
                    }

                    // When a packet has been received that we can't fully read, we need to store the decrypted length.
                    // Otherwise, we would keep decrypting the same bytes and mutating the cipher state, messing up the entire state.
                    length = previousLength = Base64Encoding.decode(new byte[]{decData[1], decData[2], decData[3]});
                } else {
                    length = previousLength;
                }
            } else {
                length = Base64Encoding.decode(new byte[]{buffer[0], buffer[1], buffer[2]});
            }

            if (length < 0) {
                throw new IllegalStateException("Decoded packet length is negative.");
            }

            if (buffer.length < length + packetLengthSize) {
                break;
            }

            int endPos = length + packetLengthSize;

            out.add(Arrays.copyOfRange(buffer, packetLengthSize, endPos));

            buffer = Arrays.copyOfRange(buffer, endPos, buffer.length);
            previousLength = 0;
        }

        return out.toArray(new byte[0][]);
    }
}
