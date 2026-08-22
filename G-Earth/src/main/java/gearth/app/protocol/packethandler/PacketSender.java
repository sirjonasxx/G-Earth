package gearth.app.protocol.packethandler;

import gearth.protocol.HMessage;

public interface PacketSender {

    void send(HMessage hMessage);

}
