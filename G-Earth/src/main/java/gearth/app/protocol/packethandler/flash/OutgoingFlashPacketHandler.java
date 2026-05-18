package gearth.app.protocol.packethandler.flash;

import gearth.misc.listenerpattern.Observable;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import gearth.app.protocol.TrafficListener;
import gearth.app.services.extension_handler.ExtensionHandler;

import java.io.IOException;
import java.io.OutputStream;

public class OutgoingFlashPacketHandler extends FlashPacketHandler {

    private final Observable<OnDatastreamConfirmedListener> datastreamConfirmedObservable;

    public OutgoingFlashPacketHandler(OutputStream outputStream, Observable<TrafficListener>[] trafficObservables, ExtensionHandler extensionHandler) {
        super(HMessage.Direction.TOSERVER, outputStream, trafficObservables, extensionHandler);

        this.datastreamConfirmedObservable = new Observable<>();
    }

    public void addOnDatastreamConfirmedListener(OnDatastreamConfirmedListener listener) {
        datastreamConfirmedObservable.addListener(listener);
    }

    private void dataStreamCheck(byte[] buffer)	{
        if (isDataStream()) {
            return;
        }

        final HPacket hpacket = new HPacket(buffer);
        final boolean isValid = (hpacket.getBytesLength() > 6 && hpacket.length() < 100);

        if (!isValid) return;

        setAsDataStream();

        final String hotelVersion = hpacket.readString();
        final String clientIdentifier = hpacket.readString();

        datastreamConfirmedObservable.fireEvent(l -> l.confirm(hotelVersion, clientIdentifier));
    }

    @Override
    public void act(byte[] buffer) throws IOException {
        dataStreamCheck(buffer);
        super.act(buffer);
    }
}
