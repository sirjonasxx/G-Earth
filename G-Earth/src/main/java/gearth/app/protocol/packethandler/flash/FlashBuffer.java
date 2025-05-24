package gearth.app.protocol.packethandler.flash;

import gearth.protocol.HPacket;
import gearth.app.protocol.crypto.RC4Cipher;
import gearth.app.protocol.packethandler.PayloadBuffer;

import java.util.ArrayList;
import java.util.Arrays;

public class FlashBuffer extends PayloadBuffer {

    @Override
    public void setCipher(RC4Cipher cipher) {
        // not needed
    }

    public byte[][] receive() {
        if (buffer.length < 6) return new byte[0][];
        HPacket total = new HPacket(buffer);

        final ArrayList<byte[]> all = new ArrayList<>();
        while (total.getBytesLength() >= 4 && total.getBytesLength() - 4 >= total.length()) {
            all.add(Arrays.copyOfRange(buffer, 0, total.length() + 4));
            buffer = Arrays.copyOfRange(buffer, total.length() + 4, buffer.length);
            total = new HPacket(buffer);
        }
        return all.toArray(new byte[0][]);
    }

}
