package gearth.services.nitro;

public interface NitroPacketModifier {

    byte[] clientToGearth(byte[] data) throws Exception;

    byte[] gearthToClient(byte[] data) throws Exception;

    byte[] serverToGearth(byte[] data) throws Exception;

    byte[] gearthToServer(byte[] data) throws Exception;

}
