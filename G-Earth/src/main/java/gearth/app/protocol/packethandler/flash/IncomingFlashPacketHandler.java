package gearth.app.protocol.packethandler.flash;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.app.protocol.TrafficListener;
import gearth.app.services.extension_handler.ExtensionHandler;

import java.io.OutputStream;

public class IncomingFlashPacketHandler extends FlashPacketHandler {

    public IncomingFlashPacketHandler(OutputStream outputStream, Observable<TrafficListener>[] trafficObservables, OutgoingFlashPacketHandler outgoingHandler, ExtensionHandler extensionHandler) {
        super(HMessage.Direction.TOCLIENT, outputStream, trafficObservables, extensionHandler);

        TrafficListener listener = new TrafficListener() {
            @Override
            public void onCapture(HMessage message) {
                if (isDataStream() && message.getPacket().structureEquals("sb") && message.getPacket().length() > 500) {
                    trafficObservables[TrafficListener.BEFORE_MODIFICATION].removeListener(this);
                    HPacket packet = message.getPacket();
                    packet.readString();
                    if (packet.readBoolean()) {
                        setEncryptedStream();
                    }
                    outgoingHandler.setEncryptedStream();
                } else if (isDataStream() && message.getPacket().structureEquals("s") && message.getPacket().length() > 200) {
                    trafficObservables[TrafficListener.BEFORE_MODIFICATION].removeListener(this);
                    outgoingHandler.setEncryptedStream();
                } else if (message.getIndex() > 1) {
                    trafficObservables[TrafficListener.BEFORE_MODIFICATION].removeListener(this);
                }
            }
        };

        trafficObservables[TrafficListener.BEFORE_MODIFICATION].addListener(listener);
    }
}
