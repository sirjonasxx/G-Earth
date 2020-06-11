package gearth.services.extensionhandler;

import gearth.protocol.HMessage;

import java.io.IOException;

public interface OnHMessageHandled {

    void finished(HMessage hMessage) throws IOException;

}
