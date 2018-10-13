package g_earth.protocol.memory.habboclient;

import g_earth.protocol.HConnection;

import java.util.List;

/**
 * Created by Jonas on 13/06/18.
 */
public abstract class HabboClient {

    protected HConnection hConnection;

    public HabboClient(HConnection connection) {
        this.hConnection = connection;
    }

    // optional
    public abstract List<byte[]> getRC4cached();

    public abstract List<byte[]> getRC4possibilities();
}
