package gearth.extensions;

public interface OnConnectionListener {
    void onConnection(String host, int port, String hotelversion, String clientType, String harbleMessagesPath);
}
