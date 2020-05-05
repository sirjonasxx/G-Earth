package gearth.protocol.packethandler;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.TrafficListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class IncomingPacketHandler extends PacketHandler {

    public IncomingPacketHandler(OutputStream outputStream, Object[] trafficObservables, OutgoingPacketHandler outgoingHandler) {
        super(outputStream, trafficObservables);

        TrafficListener listener = new TrafficListener() {
            @Override
            public void onCapture(HMessage message) {
                if (isDataStream && message.getPacket().structureEquals("s,b") && message.getPacket().length() > 500) {
                    ((Observable<TrafficListener>)trafficObservables[0]).removeListener(this);
                    HPacket packet = message.getPacket();
                    packet.readString();
                    isEncryptedStream = packet.readBoolean();
                    outgoingHandler.isEncryptedStream = true;
                }
                else if (message.getIndex() > 3) {
                    ((Observable<TrafficListener>)trafficObservables[0]).removeListener(this);
                }
            }
        };

        ((Observable<TrafficListener>)trafficObservables[0]).addListener(listener);
    }

    @Override
    public HMessage.Direction getMessageSide() {
        return HMessage.Direction.TOCLIENT;
    }

    @Override
    protected void printForDebugging(byte[] bytes) {
        System.out.println("-- DEBUG INCOMING -- " + new HPacket(bytes).toString() + " -- DEBUG --");
    }
}
