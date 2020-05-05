package gearth.protocol.packethandler;

import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class OutgoingPacketHandler extends PacketHandler {

    public OutgoingPacketHandler(OutputStream outputStream, Object[] trafficObservables) {
        super(outputStream, trafficObservables);
    }

    private List<OnDatastreamConfirmedListener> onDatastreamConfirmedListeners = new ArrayList<>();
    public void addOnDatastreamConfirmedListener(OnDatastreamConfirmedListener listener) {
        onDatastreamConfirmedListeners.add(listener);
    }
    public interface OnDatastreamConfirmedListener {
        void confirm(String hotelVersion);
    }

    private void dataStreamCheck(byte[] buffer)	{
        if (!isDataStream) {
            HPacket hpacket = new HPacket(buffer);
            isDataStream = (hpacket.getBytesLength() > 6 && hpacket.length() < 100);
            if (isDataStream) {
                String version = hpacket.readString();
                for (OnDatastreamConfirmedListener listener : onDatastreamConfirmedListeners) {
                    listener.confirm(version);
                }
            }
        }
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        dataStreamCheck(buffer);
        super.act(buffer);
    }

    @Override
    public HMessage.Direction getMessageSide() {
        return HMessage.Direction.TOSERVER;
    }


    @Override
    protected void printForDebugging(byte[] bytes) {
        System.out.println("-- DEBUG OUTGOING -- " + new HPacket(bytes).toString() + " -- DEBUG --");
    }
}
