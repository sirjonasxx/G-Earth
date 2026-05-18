package gearth.app.protocol.connection.proxy.nitro.websocket;

import gearth.app.protocol.connection.proxy.http.WebSession;

public class NitroNettySessionProvider implements NitroSessionProvider {

    private WebSession session;

    public void setSession(WebSession session) {
        this.session = session;
    }

    @Override
    public WebSession getSession() {
        return this.session;
    }

}
