package gearth.protocol.packethandler;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.TrafficListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class IncomingHandler extends Handler {

    public IncomingHandler(OutputStream outputStream, Object[] listeners) {
        super(outputStream, listeners);

        TrafficListener listener = new TrafficListener() {
            @Override
            public void onCapture(HMessage message) {
                if (isDataStream && message.getPacket().structureEquals("s,b")) {
                    ((List<TrafficListener>)listeners[0]).remove(this);
                    HPacket packet = message.getPacket();
                    packet.readString();
                    isEncryptedStream = packet.readBoolean();
                }
                else if (message.getIndex() > 3) {
                    ((List<TrafficListener>)listeners[0]).remove(this);
                }
            }
        };

        ((List<TrafficListener>)listeners[0]).add(listener);
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        if (isDataStream)	{
            continuedAct(buffer);
        }
        else  {
            out.write(buffer);
        }
    }

    @Override
    public HMessage.Side getMessageSide() {
        return HMessage.Side.TOCLIENT;
    }

    @Override
    protected void printForDebugging(byte[] bytes) {
        System.out.println("-- DEBUG INCOMING -- " + new HPacket(bytes).toString() + " -- DEBUG --");
    }
}
