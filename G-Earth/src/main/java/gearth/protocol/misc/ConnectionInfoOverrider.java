package gearth.protocol.misc;

import gearth.protocol.HConnection;

/**
 * Created by Jeunez on 30/01/2019.
 */
public interface ConnectionInfoOverrider {

    boolean mustOverrideConnection();
    HConnection.Proxy getOverrideProxy();
}
