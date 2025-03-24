package gearth.protocol.connection.proxy.nitro.websocket;

public class NitroNettySessionProvider implements NitroSessionProvider {

    private NitroSession session;

    public void setSession(NitroSession session) {
        this.session = session;
    }

    @Override
    public NitroSession getSession() {
        return this.session;
    }

}
