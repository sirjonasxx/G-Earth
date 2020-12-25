package gearth.protocol.packethandler.flash;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.protocol.TrafficListener;
import gearth.services.extensionhandler.ExtensionHandler;

import java.io.OutputStream;

public class IncomingFlashPacketHandler extends FlashPacketHandler {

    public IncomingFlashPacketHandler(OutputStream outputStream, Object[] trafficObservables, OutgoingFlashPacketHandler outgoingHandler, ExtensionHandler extensionHandler) {
        super(outputStream, trafficObservables, extensionHandler);

        TrafficListener listener = new TrafficListener() {
            @Override
            public void onCapture(HMessage message) {
                if (isDataStream && message.getPacket().structureEquals("sb") && message.getPacket().length() > 500) {
                    ((Observable<TrafficListener>)trafficObservables[0]).removeListener(this);
                    HPacket packet = message.getPacket();
                    packet.readString();
                    isEncryptedStream = packet.readBoolean();
                    outgoingHandler.isEncryptedStream = true;
                }
                else if (isDataStream && message.getPacket().structureEquals("s") && message.getPacket().length() > 200) {
                    ((Observable<TrafficListener>)trafficObservables[0]).removeListener(this);
                    outgoingHandler.isEncryptedStream = true;
                }
                else if (message.getIndex() > 1) {
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
