package gearth.app.protocol.packethandler.flash;

public interface OnDatastreamConfirmedListener {

    void confirm(String hotelVersion, String clientIdentifier);

}
