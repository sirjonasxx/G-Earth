package gearth.app.protocol.connection;

public enum HState {
    NOT_CONNECTED,
    PREPARING,          // DOMAIN AND PORT BEEN PASSED
    PREPARED,           // FOUND IP ADDRESS OF DOMAIN
    WAITING_FOR_CLIENT, // WAITING FOR CORRECT TCP CONNECTION TO BE SET UP
    CONNECTED,          // CONNECTED
    ABORTING
}