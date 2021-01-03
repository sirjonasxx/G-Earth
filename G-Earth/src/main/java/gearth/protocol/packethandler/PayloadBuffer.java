package gearth.protocol.packethandler;

import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.Arrays;

public class PayloadBuffer {

    private byte[] buffer = new byte[0];

    public HPacket[] pushAndReceive(byte[] tcpData){
        push(tcpData);
        return receive();
    }
    public void push(byte[] tcpData) {
        buffer = buffer.length == 0 ? tcpData.clone() : ByteArrayUtils.combineByteArrays(buffer, tcpData);
    }
    public HPacket[] receive() {
        if (buffer.length < 6) return new HPacket[0];
        HPacket total = new HPacket(buffer);

        ArrayList<HPacket> all = new ArrayList<>();
        while (total.getBytesLength() >= 4 && total.getBytesLength() - 4 >= total.length()){
            all.add(new HPacket(Arrays.copyOfRange(buffer, 0, total.length() + 4)));
            buffer = Arrays.copyOfRange(buffer, total.length() + 4, buffer.length);
            total = new HPacket(buffer);
        }
        return all.toArray(new HPacket[all.size()]);
    }


    public byte[] peak() {
        return buffer;
    }
    public byte[] forceClear() {
        byte[] buff = buffer;
        buffer = new byte[0];
        return buff;
    }

}
