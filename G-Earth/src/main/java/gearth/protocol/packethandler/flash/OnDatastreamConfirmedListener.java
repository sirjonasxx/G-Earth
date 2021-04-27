package gearth.protocol.packethandler.flash;

public interface OnDatastreamConfirmedListener {

    void confirm(String hotelVersion, String clientIdentifier);

}
